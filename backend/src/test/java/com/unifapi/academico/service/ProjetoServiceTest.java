package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.api.dto.response.ProjetoDetailResponseDTO;
import com.unifapi.academico.api.dto.response.ProjetoPageResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("ProjetoService")
class ProjetoServiceTest {

    @Mock
    private ProjetoRepository projetoRepository;

    @InjectMocks
    private ProjetoService projetoService;

    private Projeto projetoTeste;
    private Usuario criadorTeste;

    @BeforeEach
    void setup() {
        criadorTeste = Usuario.builder()
            .id(1L)
            .email("carlos@unifapi.edu.br")
            .nome("Carlos Eduardo")
            .tipo(TipoUsuario.ALUNO)
            .ativo(true)
            .build();

        projetoTeste = Projeto.builder()
            .id(1L)
            .titulo("Sistema de Gestão de Biblioteca")
            .descricao("Desenvolver um sistema web para gerenciar bibliotecas universitárias com funcionalidades de empréstimo e devolução")
            .status(StatusProjeto.ABERTO)
            .vagas(3)  
            .vagasPreenchidas(0)  
            .criador(criadorTeste)
            .ativo(true)
            .habilidadesNecessarias(new HashSet<>())
            .membros(new HashSet<>())
            .candidaturas(new HashSet<>())
            .criadoEm(LocalDateTime.now())
            .atualizadoEm(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("Deve listar projetos ativos com paginação")
    void deveListarProjetosComPaginacao() {
        // Arrange
        Page<Projeto> pageProjetos = new PageImpl<>(
            Arrays.asList(projetoTeste),
            Pageable.ofSize(10),
            1
        );

        when(projetoRepository.findByAtivoTrue(any(Pageable.class)))
            .thenReturn(pageProjetos);

        // Act
        ProjetoPageResponseDTO resultado = projetoService.findAllWithDetails(Pageable.ofSize(10));

        // Assert
        assertThat(resultado)
            .isNotNull();
        
        assertThat(resultado.getDados())
            .isNotEmpty()
            .hasSize(1);

        verify(projetoRepository).findByAtivoTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("Deve encontrar projeto por ID com detalhes")
    void deveEncontrarProjetoPorId() {
        // Arrange
        when(projetoRepository.findById(1L))
            .thenReturn(Optional.of(projetoTeste));

        // Act
        Optional<ProjetoDetailResponseDTO> resultado = projetoService.findByIdDetail(1L);

        // Assert
        assertThat(resultado)
            .isPresent();
        
        assertThat(resultado.get())
            .extracting("id", "titulo")
            .contains(1L, "Sistema de Gestão de Biblioteca");

        verify(projetoRepository).findById(1L);
    }

    @Test
    @DisplayName("Deve retornar vazio quando projeto não existe")
    void deveRetornarVazioProjetoInexistente() {
        // Arrange
        when(projetoRepository.findById(999L))
            .thenReturn(Optional.empty());

        // Act
        Optional<ProjetoDetailResponseDTO> resultado = projetoService.findByIdDetail(999L);

        // Assert
        assertThat(resultado)
            .isEmpty();

        verify(projetoRepository).findById(999L);
    }

    @Test
    @DisplayName("Deve criar projeto com sucesso")
    void devecriarProjetoComSucesso() {
        // Arrange
        when(projetoRepository.save(any(Projeto.class)))
            .thenReturn(projetoTeste);

        // Act
        Projeto resultado = projetoService.create(projetoTeste);

        // Assert
        assertThat(resultado)
            .isNotNull()
            .isEqualTo(projetoTeste);

        assertThat(resultado.getVagas())
            .isEqualTo(3);

        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Deve falhar ao criar projeto com 0 vagas")
    void devefalharcriarProjetoSemVagas() {
        // Arrange
        projetoTeste.setVagas(0);

        // Act & Assert
        assertThatThrownBy(() -> projetoService.create(projetoTeste))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Projeto deve ter no mínimo 1 vaga");

        verify(projetoRepository, never()).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Deve atualizar projeto")
    void deveAtualizarProjeto() {
        // Arrange
        Projeto projetoAtualizado = Projeto.builder()
            .id(1L)
            .titulo("Novo Titulo")
            .descricao("Nova descrição com mais de 20 caracteres para validação")
            .status(StatusProjeto.EM_ANDAMENTO)
            .vagas(5)
            .vagasPreenchidas(0)
            .criador(criadorTeste)
            .build();

        when(projetoRepository.findById(1L))
            .thenReturn(Optional.of(projetoTeste));
        
        when(projetoRepository.save(any(Projeto.class)))
            .thenReturn(projetoAtualizado);

        // Act
        Projeto resultado = projetoService.update(1L, projetoAtualizado);

        // Assert
        assertThat(resultado)
            .isNotNull()
            .extracting("titulo", "status", "vagas")
            .contains("Novo Titulo", StatusProjeto.EM_ANDAMENTO, 5);

        verify(projetoRepository).findById(1L);
        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Deve falhar ao atualizar projeto inexistente")
    void devefalharAtualizarProjetoInexistente() {
        // Arrange
        when(projetoRepository.findById(999L))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projetoService.update(999L, projetoTeste))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Projeto não encontrado");
    }

    @Test
    @DisplayName("Deve deletar projeto")
    void deveDeletarProjeto() {
        // Arrange
        doNothing().when(projetoRepository).deleteById(1L);

        // Act
        projetoService.delete(1L);

        // Assert
        verify(projetoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Deve listar projetos por criador")
    void deveListarProjetosPorCriador() {
        // Arrange
        when(projetoRepository.findByCriadorId(1L))
            .thenReturn(Arrays.asList(projetoTeste));

        // Act
        var resultado = projetoService.findByCriador(1L);

        // Assert
        assertThat(resultado)
            .isNotEmpty()
            .hasSize(1)
            .contains(projetoTeste);

        verify(projetoRepository).findByCriadorId(1L);
    }

    @Test
    @DisplayName("Deve listar projetos por status com paginação")
    void deveListarProjetosPorStatus() {
        // Arrange
        Page<Projeto> pageProjetos = new PageImpl<>(
            Arrays.asList(projetoTeste),
            Pageable.ofSize(10),
            1
        );

        when(projetoRepository.findByStatus(StatusProjeto.ABERTO, Pageable.ofSize(10)))
            .thenReturn(pageProjetos);

        // Act
        ProjetoPageResponseDTO resultado = projetoService.findByStatusWithDetails(
            StatusProjeto.ABERTO, 
            Pageable.ofSize(10)
        );

        // Assert
        assertThat(resultado)
            .isNotNull();
        
        assertThat(resultado.getDados())
            .isNotEmpty();

        verify(projetoRepository).findByStatus(StatusProjeto.ABERTO, Pageable.ofSize(10));
    }

    @Test
    @DisplayName("Deve decrementar vagas ao aceitar candidato")
    void deveDecrementarVagas() {
        // Arrange
        projetoTeste.setVagasPreenchidas(0);
        projetoTeste.setVagas(3);

        when(projetoRepository.findById(1L))
            .thenReturn(Optional.of(projetoTeste));
        
        when(projetoRepository.save(any(Projeto.class)))
            .thenReturn(projetoTeste);

        // Act
        projetoService.decrementarVagas(1L);

        // Assert
        assertThat(projetoTeste.getVagasPreenchidas())
            .isEqualTo(1);

        verify(projetoRepository).findById(1L);
        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Não deve decrementar vagas se todas preenchidas")
    void naoDeveDecrementarVagasSeTodasPreenchidas() {
        // Arrange
        projetoTeste.setVagasPreenchidas(3);
        projetoTeste.setVagas(3);

        when(projetoRepository.findById(1L))
            .thenReturn(Optional.of(projetoTeste));

        // Act
        projetoService.decrementarVagas(1L);

        // Assert
        assertThat(projetoTeste.getVagasPreenchidas())
            .isEqualTo(3);

        verify(projetoRepository).findById(1L);
        verify(projetoRepository, never()).save(any(Projeto.class));
    }
}