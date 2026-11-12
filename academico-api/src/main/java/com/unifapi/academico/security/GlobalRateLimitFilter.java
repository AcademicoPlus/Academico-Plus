package com.unifapi.academico.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de requisições por IP em toda a API — sem isso, qualquer endpoint
 * (não só login/OTP) aceita volume ilimitado de requisições. Usa
 * request.getRemoteAddr() (não X-Forwarded-For: esse header é enviado pelo
 * cliente e pode ser forjado para burlar o limite). Atrás do proxy do PaaS, o
 * IP real chega aqui porque application.properties define
 * server.forward-headers-strategy=native: o RemoteIpValve do Tomcat só confia
 * no X-Forwarded-For quando a conexão vem de um proxy de rede interna.
 *
 * <p>Além do limite geral por minuto, {@code POST /auth/cadastro} tem um limite
 * próprio e bem mais apertado por hora. O cadastro responde "e-mail já
 * cadastrado" quando o endereço existe — decisão consciente, pela clareza para
 * quem esqueceu que já tinha conta —, e isso transforma o endpoint num oráculo
 * de enumeração de usuários. O limite por hora é o que torna varrer uma lista
 * de e-mails inviável na prática. Ver README, seção "Rate limiting".
 *
 * <p>Os contadores são em memória: isso só funciona com uma instância da
 * aplicação. Ver a mesma ressalva em {@link RateLimiterService}.
 */
@Component
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    /** Sem o context-path (/api) — getServletPath() já o remove. */
    private static final String CAMINHO_CADASTRO = "/auth/cadastro";
    private static final String CAMINHO_SOLICITAR_RESET = "/auth/reset-senha/solicitar";

    private final int limiteRequisicoesPorJanela;
    private final Duration janela = Duration.ofMinutes(1);

    private final int limiteCadastrosPorJanela;
    private final Duration janelaDeUmaHora = Duration.ofHours(1);

    /**
     * Pedido de código de recuperação por IP. O AuthService já limita por
     * e-mail; este teto impede um único IP de pedir códigos para uma lista de
     * e-mails diferentes e esgotar a cota diária do provedor de e-mail.
     */
    private final int limiteCodigosResetPorJanela;

    private final ConcurrentHashMap<String, Contador> contadoresPorIp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Contador> contadoresCadastroPorIp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Contador> contadoresResetPorIp = new ConcurrentHashMap<>();

    public GlobalRateLimitFilter(
            @Value("${app.rate-limit.requisicoes-por-minuto:120}") int limiteRequisicoesPorJanela,
            @Value("${app.rate-limit.cadastros-por-hora:10}") int limiteCadastrosPorJanela,
            @Value("${app.rate-limit.codigos-reset-por-hora:10}") int limiteCodigosResetPorJanela
    ) {
        this.limiteRequisicoesPorJanela = limiteRequisicoesPorJanela;
        this.limiteCadastrosPorJanela = limiteCadastrosPorJanela;
        this.limiteCodigosResetPorJanela = limiteCodigosResetPorJanela;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = request.getRemoteAddr();

        if (excedeuLimite(contadoresPorIp, ip, limiteRequisicoesPorJanela, janela)) {
            responderExcesso(response, "Muitas requisições. Tente novamente em instantes.");
            return;
        }

        if (ehTentativaDeCadastro(request)
                && excedeuLimite(contadoresCadastroPorIp, ip, limiteCadastrosPorJanela, janelaDeUmaHora)) {
            responderExcesso(response, "Muitas tentativas de cadastro. Tente novamente mais tarde.");
            return;
        }

        if (ehPost(request, CAMINHO_SOLICITAR_RESET)
                && excedeuLimite(contadoresResetPorIp, ip, limiteCodigosResetPorJanela, janelaDeUmaHora)) {
            responderExcesso(response, "Muitos pedidos de código. Tente novamente mais tarde.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean ehTentativaDeCadastro(HttpServletRequest request) {
        return ehPost(request, CAMINHO_CADASTRO);
    }

    private static boolean ehPost(HttpServletRequest request, String caminho) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && caminho.equals(request.getServletPath());
    }

    private static boolean excedeuLimite(
            ConcurrentHashMap<String, Contador> contadores,
            String ip,
            int limite,
            Duration janela
    ) {
        Instant agora = Instant.now();
        Contador contador = contadores.compute(ip, (chave, atual) -> {
            if (atual == null || atual.janelaExpiraEm.isBefore(agora)) {
                atual = new Contador();
                atual.janelaExpiraEm = agora.plus(janela);
            }
            atual.contagem++;
            return atual;
        });
        return contador.contagem > limite;
    }

    private static void responderExcesso(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.getWriter().write("{\"erro\": \"" + mensagem + "\"}");
    }

    /** Evita que os mapas cresçam indefinidamente com IPs que pararam de acessar. */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void limparExpirados() {
        Instant agora = Instant.now();
        contadoresPorIp.entrySet().removeIf(entry -> entry.getValue().janelaExpiraEm.isBefore(agora));
        contadoresCadastroPorIp.entrySet().removeIf(entry -> entry.getValue().janelaExpiraEm.isBefore(agora));
        contadoresResetPorIp.entrySet().removeIf(entry -> entry.getValue().janelaExpiraEm.isBefore(agora));
    }

    private static class Contador {
        int contagem;
        Instant janelaExpiraEm;
    }
}
