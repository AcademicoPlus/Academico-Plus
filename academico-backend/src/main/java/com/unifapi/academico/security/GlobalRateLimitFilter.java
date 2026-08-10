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
 * cliente e pode ser forjado para burlar o limite). Se a aplicação for
 * publicada atrás de um proxy reverso, configure
 * server.forward-headers-strategy=framework para que o Spring/Tomcat resolva
 * o IP real do cliente de forma confiável antes de chegar aqui.
 */
@Component
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private final int limiteRequisicoesPorJanela;
    private final Duration janela = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Contador> contadoresPorIp = new ConcurrentHashMap<>();

    public GlobalRateLimitFilter(
            @Value("${app.rate-limit.requisicoes-por-minuto:120}") int limiteRequisicoesPorJanela
    ) {
        this.limiteRequisicoesPorJanela = limiteRequisicoesPorJanela;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (excedeuLimite(request.getRemoteAddr())) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"erro\": \"Muitas requisições. Tente novamente em instantes.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean excedeuLimite(String ip) {
        Instant agora = Instant.now();
        Contador contador = contadoresPorIp.compute(ip, (chave, atual) -> {
            if (atual == null || atual.janelaExpiraEm.isBefore(agora)) {
                atual = new Contador();
                atual.janelaExpiraEm = agora.plus(janela);
            }
            atual.contagem++;
            return atual;
        });
        return contador.contagem > limiteRequisicoesPorJanela;
    }

    /** Evita que o mapa cresça indefinidamente com IPs que pararam de acessar. */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void limparExpirados() {
        Instant agora = Instant.now();
        contadoresPorIp.entrySet().removeIf(entry -> entry.getValue().janelaExpiraEm.isBefore(agora));
    }

    private static class Contador {
        int contagem;
        Instant janelaExpiraEm;
    }
}
