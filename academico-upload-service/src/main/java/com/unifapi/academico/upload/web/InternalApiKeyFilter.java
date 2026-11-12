package com.unifapi.academico.upload.web;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Este serviço não é exposto ao público — só o monolito deve chamá-lo. Sem
 * service mesh/mTLS disponível no plano de PaaS usado, uma chave compartilhada
 * simples no header cobre essa necessidade. Em produção, publique-o também como
 * serviço privado (Render: Private Service), sem URL pública — ver DEPLOY.md.
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Internal-Api-Key";

    /** Valor de exemplo do .env.example do monolito — nunca deve chegar em produção. */
    private static final String CHAVE_DE_EXEMPLO = "changeme-shared-secret";
    private static final int TAMANHO_MINIMO_RECOMENDADO = 32;

    @Value("${app.internal-api-key}")
    private String internalApiKey;

    @PostConstruct
    void avisarSeChaveFraca() {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            log.error("INTERNAL_API_KEY vazia: todas as requisições serão recusadas (401).");
        } else if (CHAVE_DE_EXEMPLO.equals(internalApiKey) || internalApiKey.length() < TAMANHO_MINIMO_RECOMENDADO) {
            log.warn("INTERNAL_API_KEY fraca (valor de exemplo ou menos de {} caracteres). "
                    + "Gere uma com: openssl rand -base64 48", TAMANHO_MINIMO_RECOMENDADO);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!chaveConfere(request.getHeader(HEADER))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Comparação em tempo constante (MessageDigest.isEqual): String.equals para
     * no primeiro caractere diferente, e o tempo de resposta vaza quantos
     * caracteres do começo já estão certos.
     */
    private boolean chaveConfere(String chaveRecebida) {
        if (internalApiKey == null || internalApiKey.isBlank() || chaveRecebida == null) {
            return false;
        }
        return MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                chaveRecebida.getBytes(StandardCharsets.UTF_8));
    }
}
