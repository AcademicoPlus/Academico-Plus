package com.unifapi.academico.upload.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Este serviço não é exposto ao público — só o monolito deve chamá-lo. Sem
 * service mesh/mTLS disponível no plano de PaaS usado, uma chave compartilhada
 * simples no header já cobre essa necessidade.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.internal-api-key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String chaveRecebida = request.getHeader("X-Internal-Api-Key");
        if (internalApiKey == null || internalApiKey.isBlank() || !internalApiKey.equals(chaveRecebida)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
