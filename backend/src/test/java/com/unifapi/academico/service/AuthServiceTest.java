package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioValido;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        usuarioValido = Usuario.builder()
            .id(1L)
            .email("teste@unifapi.edu.br")
            .nome("Teste User")
            .curso("Computação")
            .tipo(TipoUsuario.ALUNO)
            .senhaHash("hashedPassword")
            .ativo(true)
            .build();
    }

    @Test
    @DisplayName("Deve fazer login com sucesso")
    void deveLoginComSucesso() {
        // Arrange
        when(usuarioRepository.findByEmail("teste@unifapi.edu.br"))
            .thenReturn(Optional.of(usuarioValido));
        
        when(passwordEncoder.matches("senha123", "hashedPassword"))
            .thenReturn(true);
        
        when(jwtUtil.generateToken(any(), any()))
            .thenReturn("jwt_token_123");

        // Act
        Map<String, Object> resultado = authService.login("teste@unifapi.edu.br", "senha123");

        // Assert
        assertNotNull(resultado);
        assertEquals("teste@unifapi.edu.br", resultado.get("email"));
        assertEquals("jwt_token_123", resultado.get("token"));
    }

    @Test
    @DisplayName("Não deve fazer login com senha incorreta")
    void naoDeveLoginComSenhaIncorreta() {
        // Arrange
        when(usuarioRepository.findByEmail("teste@unifapi.edu.br"))
            .thenReturn(Optional.of(usuarioValido));
        
        when(passwordEncoder.matches("senhaErrada", "hashedPassword"))
            .thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authService.login("teste@unifapi.edu.br", "senhaErrada");
        });
    }

    @Test
    @DisplayName("Deve fazer signup com sucesso")
    void deveSignupComSucesso() {
        // Arrange
        when(usuarioRepository.existsByEmail("novo@unifapi.edu.br"))
            .thenReturn(false);
        
        when(passwordEncoder.encode("senha123"))
            .thenReturn("encoded_senha");
        
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(usuarioValido);
        
        when(jwtUtil.generateToken(any(), any()))
            .thenReturn("jwt_token_123");

        // Act
        Map<String, Object> resultado = authService.signup(
            "novo@unifapi.edu.br",
            "Novo User",
            "Computação",
            "senha123"
        );

        // Assert
        assertNotNull(resultado);
        assertEquals("teste@unifapi.edu.br", resultado.get("email"));
    }

    @Test
    @DisplayName("Não deve fazer signup com email duplicado")
    void naoDeveSignupComEmailDuplicado() {
        // Arrange
        when(usuarioRepository.existsByEmail("teste@unifapi.edu.br"))
            .thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authService.signup("teste@unifapi.edu.br", "User", "Curso", "senha123");
        });
    }
}