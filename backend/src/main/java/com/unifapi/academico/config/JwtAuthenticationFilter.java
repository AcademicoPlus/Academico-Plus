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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority; // 👈 1. IMPORT NOVO
import org.springframework.security.core.authority.SimpleGrantedAuthority; // 👈 2. IMPORT NOVO
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final HandlerExceptionResolver resolver;
    private final TokenBlacklistService tokenBlacklistService; 

    public JwtAuthenticationFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
            TokenBlacklistService tokenBlacklistService) { 
        this.resolver = resolver;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        String token = extractToken(request);

        if (token != null) {
            try {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    throw new BadCredentialsException("Token inválido ou sessão encerrada.");
                }

                Claims claims = parseToken(token);
                
                // 🔴 ALTERAÇÃO AQUI: Extrai a claim "tipo" (ou "role") colocada no token durante o login
                String tipo = claims.get("tipo", String.class); // ex: "ADMIN", "ALUNO", "PROFESSOR"
                
                List<GrantedAuthority> authorities = new ArrayList<>();
                if (tipo != null) {
                    // O Spring Security exige o prefixo "ROLE_" para conferir com @PreAuthorize("hasRole('ADMIN')")
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + tipo.toUpperCase()));
                }

                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),  // email/username
                        null,
                        authorities           // 👈 PASSAMOS AS AUTHORITIES AQUI (Em vez de new ArrayList<>())
                    );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception ex) {
                resolver.resolveException(request, response, null, ex);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}