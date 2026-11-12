package com.unifapi.academico.security;

import com.unifapi.academico.exception.RegraDeNegocioException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador de tentativas em memória, usado para travar login e validação de
 * OTP contra força bruta. Em memória porque a aplicação roda em instância
 * única — se um dia escalar horizontalmente, isso precisa virar um contador
 * compartilhado (Redis, por exemplo), senão cada instância teria seu próprio
 * limite independente.
 */
@Component
public class RateLimiterService {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Tentativas> tentativasPorChave = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Cota> cotasPorChave = new ConcurrentHashMap<>();

    /** Chamar antes de tentar autenticar. Lança se a chave estiver bloqueada. */
    public void verificarDisponivel(String chave) {
        Tentativas tentativas = tentativasPorChave.get(normalizar(chave));
        if (tentativas != null && tentativas.bloqueadoAte != null && tentativas.bloqueadoAte.isAfter(Instant.now())) {
            throw new RegraDeNegocioException("Muitas tentativas. Tente novamente mais tarde.");
        }
    }

    public void registrarFalha(String chave) {
        registrarFalha(chave, MAX_TENTATIVAS);
    }

    /** Igual a {@link #registrarFalha(String)}, com um teto próprio para esta chave. */
    public void registrarFalha(String chave, int maxTentativas) {
        tentativasPorChave.compute(normalizar(chave), (chaveNormalizada, atual) -> {
            Instant agora = Instant.now();
            if (atual == null || atual.janelaExpiraEm.isBefore(agora)) {
                atual = new Tentativas();
                atual.janelaExpiraEm = agora.plus(JANELA);
            }
            atual.contador++;
            if (atual.contador >= maxTentativas) {
                atual.bloqueadoAte = agora.plus(BLOQUEIO);
            }
            return atual;
        });
    }

    /** Zera o contador — chamar sempre que a credencial/código informado for válido. */
    public void registrarSucesso(String chave) {
        tentativasPorChave.remove(normalizar(chave));
    }

    /**
     * Cota de uso por janela (não é falha: conta toda chamada). Lança quando a
     * chave já usou {@code limite} vezes dentro de {@code janela}. Usado para
     * limitar pedidos de código por e-mail — sem isso dava para inundar a caixa
     * de alguém e esgotar a cota diária do provedor de e-mail.
     */
    public void consumirCota(String chave, int limite, Duration janela, String mensagemQuandoExcede) {
        Instant agora = Instant.now();
        Cota cota = cotasPorChave.compute(normalizar(chave), (chaveNormalizada, atual) -> {
            if (atual == null || atual.janelaExpiraEm.isBefore(agora)) {
                atual = new Cota();
                atual.janelaExpiraEm = agora.plus(janela);
            }
            atual.usos++;
            return atual;
        });
        if (cota.usos > limite) {
            throw new RegraDeNegocioException(mensagemQuandoExcede);
        }
    }

    /** Evita que o mapa cresça indefinidamente com chaves que nunca mais tentaram de novo. */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void limparExpirados() {
        Instant agora = Instant.now();
        cotasPorChave.entrySet().removeIf(entry -> entry.getValue().janelaExpiraEm.isBefore(agora));
        tentativasPorChave.entrySet().removeIf(entry -> {
            Tentativas t = entry.getValue();
            boolean bloqueioExpirado = t.bloqueadoAte == null || t.bloqueadoAte.isBefore(agora);
            boolean janelaExpirada = t.janelaExpiraEm.isBefore(agora);
            return bloqueioExpirado && janelaExpirada;
        });
    }

    private static String normalizar(String chave) {
        return chave == null ? "" : chave.trim().toLowerCase();
    }

    private static class Tentativas {
        int contador;
        Instant janelaExpiraEm;
        Instant bloqueadoAte;
    }

    private static class Cota {
        int usos;
        Instant janelaExpiraEm;
    }
}
