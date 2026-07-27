package com.unifapi.academico.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final HandlerExceptionResolver resolver;

    // Injetamos o HandlerExceptionResolver para repassar erros de JWT ao GlobalExceptionHandler
    public JwtAuthenticationFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        String token = extractToken(request);

        if (token != null) {
            try {
                // Se o token estiver expirado ou for inválido, o parseToken lança uma exceção aqui
                Claims claims = parseToken(token);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),  // email/username
                        null,
                        new ArrayList<>()     // authorities/roles
                    );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception ex) {
                // Repassa a exceção (ExpiredJwtException, MalformedJwtException, etc.) para o RestControllerAdvice
                resolver.resolveException(request, response, null, ex);
                return; // Interrompe a requisição aqui para não rodar o restaste da cadeia de filtros
            }
        }
        
        // Se não há token no header, deixa passar para o Spring Security responder com 401 via EntryPoint
        filterChain.doFilter(request, response);
    }

    /**
     * Extrai o token do header Authorization ("Bearer <token>")
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }

    /**
     * Valida e extrai os claims do token.
     * Lança exceções do JJWT se o token for inválido, adulterado ou expirado.
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}