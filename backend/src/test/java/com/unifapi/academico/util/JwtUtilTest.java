package com.unifapi.academico.util;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JwtUtil")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    private String email;
    private Map<String, Object> claims;

    @BeforeEach
    void setup() {
        email = "teste@unifapi.edu.br";
        claims = new HashMap<>();
        claims.put("tipo", "ALUNO");
    }

    @Test
    @DisplayName("Deve gerar token válido")
    void deveGerarTokenValido() {
        // Act
        String token = jwtUtil.generateToken(email, claims);

        // Assert
        assertThat(token)
            .isNotNull()
            .isNotEmpty()
            .contains(".");
    }

    @Test
    @DisplayName("Deve extrair email do token")
    void deveExtrairEmailDoToken() {
        // Arrange
        String token = jwtUtil.generateToken(email, claims);

        // Act
        String emailExtraido = jwtUtil.extractEmail(token);

        // Assert
        assertThat(emailExtraido)
            .isEqualTo(email);
    }

    @Test
    @DisplayName("Deve validar token válido")
    void deveValidarTokenValido() {
        // Arrange
        String token = jwtUtil.generateToken(email, claims);

        // Act
        boolean valido = jwtUtil.isTokenValid(token, email);

        // Assert
        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("Deve falhar validação com email diferente")
    void devefalharValidacaoComEmailDiferente() {
        // Arrange
        String token = jwtUtil.generateToken(email, claims);

        // Act
        boolean valido = jwtUtil.isTokenValid(token, "outro@unifapi.edu.br");

        // Assert
        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("Deve lançar exceção para token inválido")
    void deveLancarExcecaoParaTokenInvalido() {
        // Arrange
        String tokenInvalido = "token.invalido.xyz";

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.validateToken(tokenInvalido))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Deve extrair claim customizado")
    void deveExtrairClaimCustomizado() {
        // Arrange
        String token = jwtUtil.generateToken(email, claims);

        // Act
        String tipo = jwtUtil.getClaimFromToken(token, "tipo");

        // Assert
        assertThat(tipo)
            .isEqualTo("ALUNO");
    }
}