package com.unifapi.academico.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMinutes;
    private final long resetTokenExpirationMinutes = 15; // 15 min fixos para reset de senha

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret:sua_chave_secreta_super_segura_com_no_minimo_32_caracteres_123}") String secret,
            @Value("${app.security.jwt.expiration-minutes:1440}") long expirationMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = expirationMinutes;
    }

    // Equivale a create_access_token() do FastAPI
    public String gerarTokenAcesso(UUID userId, String role) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("type", "ACCESS")
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(secretKey)
                .compact();
    }

    public String gerarTokenResetSenha(UUID userId) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(resetTokenExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "RESET_PASSWORD")
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(secretKey)
                .compact();
    }

    public boolean validarToken(String token, String tipoEsperado) {
        try {
            Claims claims = extrairClaims(token);
            String tipoToken = claims.get("type", String.class);
            return tipoEsperado.equals(tipoToken);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validarTokenReset(String token) {
        return validarToken(token, "RESET_PASSWORD");
    }

    public UUID obterSubjectDoToken(String token) {
        Claims claims = extrairClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String obterRoleDoToken(String token) {
        Claims claims = extrairClaims(token);
        return claims.get("role", String.class);
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}