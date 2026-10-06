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

    /** Chamar antes de tentar autenticar. Lança se a chave estiver bloqueada. */
    public void verificarDisponivel(String chave) {
        Tentativas tentativas = tentativasPorChave.get(normalizar(chave));
        if (tentativas != null && tentativas.bloqueadoAte != null && tentativas.bloqueadoAte.isAfter(Instant.now())) {
            throw new RegraDeNegocioException("Muitas tentativas. Tente novamente mais tarde.");
        }
    }

    public void registrarFalha(String chave) {
        tentativasPorChave.compute(normalizar(chave), (chaveNormalizada, atual) -> {
            Instant agora = Instant.now();
            if (atual == null || atual.janelaExpiraEm.isBefore(agora)) {
                atual = new Tentativas();
                atual.janelaExpiraEm = agora.plus(JANELA);
            }
            atual.contador++;
            if (atual.contador >= MAX_TENTATIVAS) {
                atual.bloqueadoAte = agora.plus(BLOQUEIO);
            }
            return atual;
        });
    }

    /** Zera o contador — chamar sempre que a credencial/código informado for válido. */
    public void registrarSucesso(String chave) {
        tentativasPorChave.remove(normalizar(chave));
    }

    /** Evita que o mapa cresça indefinidamente com chaves que nunca mais tentaram de novo. */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void limparExpirados() {
        Instant agora = Instant.now();
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
}
