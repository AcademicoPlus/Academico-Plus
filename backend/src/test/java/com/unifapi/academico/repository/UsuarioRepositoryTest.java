package com.unifapi.academico.repository;

import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.domain.enums.TipoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UsuarioRepository")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuarioTeste;

    @BeforeEach
    void setup() {
        usuarioTeste = Usuario.builder()
            .email("teste@unifapi.edu.br")
            .nome("Teste User")
            .curso("Computação")
            .senhaHash("hashedPassword")
            .tipo(TipoUsuario.ALUNO)
            .ativo(true)
            .build();

        usuarioRepository.save(usuarioTeste);
    }

    @Test
    @DisplayName("Deve encontrar usuario por email")
    void deveEncontrarUsuarioPorEmail() {
        // Act
        Optional<Usuario> resultado = usuarioRepository.findByEmail("teste@unifapi.edu.br");

        // Assert
        assertThat(resultado)
            .isPresent()
            .contains(usuarioTeste);
    }

    @Test
    @DisplayName("Deve retornar vazio para email inexistente")
    void deveRetornarVazioParaEmailInexistente() {
        // Act
        Optional<Usuario> resultado = usuarioRepository.findByEmail("inexistente@unifapi.edu.br");

        // Assert
        assertThat(resultado)
            .isEmpty();
    }

    @Test
    @DisplayName("Deve verificar se email já existe")
    void deveVerificarSeEmailJaExiste() {
        // Act
        boolean existe = usuarioRepository.existsByEmail("teste@unifapi.edu.br");

        // Assert
        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false para email que não existe")
    void deveRetornarFalseParaEmailQueNaoExiste() {
        // Act
        boolean existe = usuarioRepository.existsByEmail("novo@unifapi.edu.br");

        // Assert
        assertThat(existe).isFalse();
    }

    @Test
    @DisplayName("Deve listar usuarios por tipo")
    void deveListarUsuariosPorTipo() {
        // Arrange
        Usuario professor = Usuario.builder()
            .email("professor@unifapi.edu.br")
            .nome("Professor")
            .senhaHash("hashPassword")
            .tipo(TipoUsuario.PROFESSOR)
            .ativo(true)
            .build();

        usuarioRepository.save(professor);

        // Act
        var usuarios = usuarioRepository.findByTipo(TipoUsuario.PROFESSOR);

        // Assert
        assertThat(usuarios)
            .isNotEmpty()
            .contains(professor);
    }
}