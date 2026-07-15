package com.unifapi.academico.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Gerar token JWT com claims customizados
     */
    public String generateToken(String email, Map<String, Object> claims) {
        return createToken(claims, email);
    }

    /**
     * Gerar token JWT simples (sem claims extras)
     */
    public String generateToken(String email) {
        return generateToken(email, new HashMap<>());
    }

    /**
     * Criar token JWT
     */
    private String createToken(Map<String, Object> claims, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Extrair email (subject) do token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrair claim específico do token
     */
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrair todos os claims do token
     */
    public Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.parser()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Validar se token é válido
     */
    public boolean isTokenValid(String token, String email) {
        try {
            final String tokenEmail = extractEmail(token);
            return (tokenEmail.equals(email) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Erro ao validar JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validar se token expirou
     */
    private boolean isTokenExpired(String token) {
        try {
            final Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Obter email do token (sem validação)
     */
    public String getEmailFromToken(String token) {
        try {
            return extractEmail(token);
        } catch (Exception e) {
            log.error("Erro ao extrair email do token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obter claim do token
     */
    public String getClaimFromToken(String token, String claimName) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get(claimName, String.class);
        } catch (Exception e) {
            log.error("Erro ao extrair claim '{}' do token: {}", claimName, e.getMessage());
            return null;
        }
    }

    /**
     * Validar token com exceção
     */
    public void validateToken(String token) throws JwtException {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
        } catch (SecurityException e) {
            log.error("JWT signature é inválida: {}", e.getMessage());
            throw new JwtException("JWT signature é inválida", e);
        } catch (MalformedJwtException e) {
            log.error("JWT inválido: {}", e.getMessage());
            throw new JwtException("JWT inválido", e);
        } catch (ExpiredJwtException e) {
            log.error("JWT expirou: {}", e.getMessage());
            throw new JwtException("JWT expirou", e);
        } catch (UnsupportedJwtException e) {
            log.error("JWT não suportado: {}", e.getMessage());
            throw new JwtException("JWT não suportado", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT vazio: {}", e.getMessage());
            throw new JwtException("JWT vazio", e);
        }
    }

    /**
     * Extrair token do header Authorization
     */
    public String extractTokenFromBearer(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }

    /**
     * Checar se token está expirado (sem validação de signature)
     */
    public boolean isTokenExpiredWithoutValidation(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Obter tempo até expiração (em ms)
     */
    public long getTimeUntilExpiration(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Erro ao calcular tempo até expiração: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Obter tempo até expiração (em segundos)
     */
    public long getSecondsUntilExpiration(String token) {
        return getTimeUntilExpiration(token) / 1000;
    }
}