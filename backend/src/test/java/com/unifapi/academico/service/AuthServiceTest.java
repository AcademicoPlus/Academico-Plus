package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    private Usuario usuarioTeste;

    @BeforeEach
    void setup() {
        usuarioTeste = Usuario.builder()
            .id(1L)
            .email("teste@unifapi.edu.br")
            .nome("Teste User")
            .senhaHash("hashedPassword")
            .ativo(true)
            .tipo(TipoUsuario.ALUNO)
            .build();
    }

    @Test
    @DisplayName("Deve fazer login com sucesso")
    void deveLoginComSucesso() {
        // Arrange
        String email = "teste@unifapi.edu.br";
        String senha = "password123";

        when(usuarioRepository.findByEmail(email))
            .thenReturn(Optional.of(usuarioTeste));
        
        when(passwordEncoder.matches(senha, usuarioTeste.getSenhaHash()))
            .thenReturn(true);
        
        when(jwtUtil.generateToken(any(), any()))
            .thenReturn("token_valido");

        // Act
        Map<String, Object> resultado = authService.login(email, senha);

        // Assert
        assertThat(resultado)
            .isNotNull()
            .containsKeys("id", "email", "token", "expiresIn");
        
        assertThat(resultado)
            .extracting("email")
            .isEqualTo(email);
        
        assertThat(resultado)
            .extracting("token")
            .isEqualTo("token_valido");

        verify(usuarioRepository).findByEmail(email);
        verify(passwordEncoder).matches(senha, usuarioTeste.getSenhaHash());
        verify(jwtUtil).generateToken(any(), any());
    }

    @Test
    @DisplayName("Deve falhar login com usuario não encontrado")
    void deveFalharLoginUsuarioNaoEncontrado() {
        // Arrange
        String email = "inexistente@unifapi.edu.br";
        String senha = "password123";

        when(usuarioRepository.findByEmail(email))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(email, senha))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Usuário não encontrado");
    }

    @Test
    @DisplayName("Deve falhar login com senha incorreta")
    void deveFalharLoginSenhaIncorreta() {
        // Arrange
        String email = "teste@unifapi.edu.br";
        String senha = "senhaErrada";

        when(usuarioRepository.findByEmail(email))
            .thenReturn(Optional.of(usuarioTeste));
        
        when(passwordEncoder.matches(senha, usuarioTeste.getSenhaHash()))
            .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(email, senha))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Senha incorreta");
    }

    @Test
    @DisplayName("Deve falhar login com usuario inativo")
    void deveFalharLoginUsuarioInativo() {
        // Arrange
        usuarioTeste.setAtivo(false);
        String email = "teste@unifapi.edu.br";
        String senha = "password123";

        when(usuarioRepository.findByEmail(email))
            .thenReturn(Optional.of(usuarioTeste));
        
        when(passwordEncoder.matches(senha, usuarioTeste.getSenhaHash()))
            .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(email, senha))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Usuário inativo");
    }

    @Test
    @DisplayName("Deve fazer signup com sucesso")
    void deveSignupComSucesso() {
        // Arrange
        String email = "novo@unifapi.edu.br";
        String nome = "Novo Usuario";
        String curso = "Computação";
        String senha = "password123";

        when(usuarioRepository.existsByEmail(email))
            .thenReturn(false);
        
        when(passwordEncoder.encode(senha))
            .thenReturn("encodedPassword");
        
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(usuarioTeste);
        
        when(jwtUtil.generateToken(any(), any()))
            .thenReturn("token_novo");

        // Act
        Map<String, Object> resultado = authService.signup(email, nome, curso, senha);

        // Assert
        assertThat(resultado)
            .isNotNull()
            .containsKeys("id", "email", "nome", "token");

        verify(usuarioRepository).existsByEmail(email);
        verify(passwordEncoder).encode(senha);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve falhar signup com email já registrado")
    void deveFalharSignupEmailJaRegistrado() {
        // Arrange
        String email = "existente@unifapi.edu.br";

        when(usuarioRepository.existsByEmail(email))
            .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.signup(email, "Nome", "Curso", "password123"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Email já registrado");
    }

    @Test
    @DisplayName("Deve falhar signup com senha muito curta")
    void deveFalharSignupSenhamuito() {
        // Arrange
        when(usuarioRepository.existsByEmail(any()))
            .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.signup(
            "novo@unifapi.edu.br", 
            "Nome", 
            "Curso", 
            "12345"  
        ))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Senha deve ter no mínimo 8 caracteres");
    }
}