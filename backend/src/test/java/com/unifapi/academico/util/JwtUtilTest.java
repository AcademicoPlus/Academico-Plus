package com.unifapi.academico.util;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("JwtUtil")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    private String email = "teste@unifapi.edu.br";
    private String token;

    @BeforeEach
    void setup() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tipo", "ALUNO");
        claims.put("id", 1L);
        token = jwtUtil.generateToken(email, claims);
    }

    @Test
    @DisplayName("Deve gerar token válido")
    void deveGerarTokenValido() {
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Deve extrair email do token")
    void deveExtrairEmailDoToken() {
        String emailExtraido = jwtUtil.extractEmail(token);
        assertEquals(email, emailExtraido);
    }

    @Test
    @DisplayName("Deve extrair claim customizado")
    void deveExtrairClaimCustomizado() {
        String tipo = jwtUtil.getClaimFromToken(token, "tipo");
        assertEquals("ALUNO", tipo);
    }

    @Test
    @DisplayName("Deve validar token válido")
    void deveValidarTokenValido() {
        boolean isValid = jwtUtil.isTokenValid(token, email);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Não deve validar token com email diferente")
    void naoDeveValidarTokenComEmailDiferente() {
        boolean isValid = jwtUtil.isTokenValid(token, "outro@unifapi.edu.br");
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Deve extrair token de bearer")
    void deveExtrairTokenDeBearer() {
        String bearer = "Bearer " + token;
        String extracted = jwtUtil.extractTokenFromBearer(bearer);
        assertEquals(token, extracted);
    }

    @Test
    @DisplayName("Deve validar token sem exceção")
    void deveValidarTokenSemExcecao() {
        assertDoesNotThrow(() -> jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Não deve validar token inválido")
    void naoDeveValidarTokenInvalido() {
        String tokenInvalido = "token.invalido.123";
        assertThrows(JwtException.class, () -> jwtUtil.validateToken(tokenInvalido));
    }

    @Test
    @DisplayName("Deve retornar tempo até expiração")
    void deveRetornarTempoAtéExpiração() {
        long seconds = jwtUtil.getSecondsUntilExpiration(token);
        assertTrue(seconds > 0);
        assertTrue(seconds <= 86400); // 24h em segundos
    }
}