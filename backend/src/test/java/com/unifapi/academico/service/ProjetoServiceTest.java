package com.unifapi.academico.service;

import com.unifapi.academico.api.dto.request.ProjetoRequestDTO;
import com.unifapi.academico.api.dto.response.ProjetoCardDTO;
import com.unifapi.academico.api.dto.response.ProjetoDetailResponseDTO;
import com.unifapi.academico.api.dto.response.ProjetoPageResponseDTO;
import com.unifapi.academico.api.mapper.ProjetoMapper;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.repository.HabilidadeRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
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
import com.unifapi.academico.exception.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjetoService Testes")
class ProjetoServiceTest {

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProjetoMapper mapper;

    @Mock
    private HabilidadeRepository habilidadeRepository;

    @InjectMocks
    private ProjetoService projetoService;

    private Projeto projetoTeste;
    private Usuario criadorTeste;
    private ProjetoRequestDTO projetoRequestDTO;

    @BeforeEach
    void setup() {
        criadorTeste = Usuario.builder()
                .id(UUID.randomUUID())
                .email("carlos@unifapi.edu.br")
                .nome("Carlos Eduardo")
                .tipo(TipoUsuario.ALUNO)
                .ativo(true)
                .build();

        projetoTeste = Projeto.builder()
                .id(UUID.randomUUID())
                .titulo("Sistema de Gestão de Biblioteca")
                .descricao("Desenvolver um sistema web para gerenciar bibliotecas universitárias")
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

        projetoRequestDTO = ProjetoRequestDTO.builder()
                .titulo("Sistema de Gestão de Biblioteca")
                .descricao("Desenvolver um sistema web para gerenciar bibliotecas universitárias")
                .vagas(3)
                .habilidadeIds(List.of())
                .build();
    }

    @Test
    @DisplayName("Deve listar projetos ativos com paginação")
    void deveListarProjetosComPaginacao() {
        Page<Projeto> pageProjetos = new PageImpl<>(
                Arrays.asList(projetoTeste),
                Pageable.ofSize(10),
                1);

        when(projetoRepository.findByAtivoTrue(any(Pageable.class)))
                .thenReturn(pageProjetos);

        ProjetoPageResponseDTO resultado = projetoService.findAllWithDetails(Pageable.ofSize(10));

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDados()).isNotEmpty().hasSize(1);

        verify(projetoRepository).findByAtivoTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("Deve encontrar projeto por ID com detalhes")
    void deveEncontrarProjetoPorId() {
        UUID id = projetoTeste.getId();
        when(projetoRepository.findById(id))
                .thenReturn(Optional.of(projetoTeste));

        Optional<ProjetoDetailResponseDTO> resultado = projetoService.findByIdDetail(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get())
                .extracting("id", "titulo")
                .contains(id, "Sistema de Gestão de Biblioteca");

        verify(projetoRepository).findById(id);
    }

    @Test
    @DisplayName("Deve retornar vazio quando projeto não existe")
    void deveRetornarVazioProjetoInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(projetoRepository.findById(idInexistente))
                .thenReturn(Optional.empty());

        Optional<ProjetoDetailResponseDTO> resultado = projetoService.findByIdDetail(idInexistente);

        assertThat(resultado).isEmpty();
        verify(projetoRepository).findById(idInexistente);
    }

    @Test
    @DisplayName("Deve criar projeto com sucesso")
    void deveCriarProjetoComSucesso() {
        // Arrange
        when(usuarioRepository.findByEmail("carlos@unifapi.edu.br"))
                .thenReturn(Optional.of(criadorTeste));

        when(projetoRepository.save(any(Projeto.class)))
                .thenReturn(projetoTeste);

        // Act
        ProjetoCardDTO resultado = projetoService.criarProjeto(projetoRequestDTO, "carlos@unifapi.edu.br");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getTitulo()).isEqualTo("Sistema de Gestão de Biblioteca");
        assertThat(resultado.getVagas()).isEqualTo(3);

        verify(usuarioRepository).findByEmail("carlos@unifapi.edu.br");
        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Deve falhar ao criar projeto com 0 vagas")
    void deveFalharCriarProjetoSemVagas() {
        // Arrange
        projetoRequestDTO.setVagas(0);

        // Act & Assert
        assertThatThrownBy(() -> projetoService.criarProjeto(projetoRequestDTO, "carlos@unifapi.edu.br"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O projeto deve ter no mínimo 1 vaga.");

        verify(projetoRepository, never()).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Deve atualizar projeto com sucesso quando for o criador")
    void deveAtualizarProjetoComSucesso() {
        // Arrange
        UUID id = projetoTeste.getId();

        ProjetoRequestDTO dtoAtualizacao = ProjetoRequestDTO.builder()
                .titulo("Sistema de Gestão de Biblioteca V2")
                .descricao("Descrição atualizada do sistema com mais de 20 caracteres para validação")
                .vagas(5)
                .habilidadeIds(List.of())
                .build();

        // 1. DTO de retorno mockado para o mapper não retornar null
        ProjetoCardDTO cardDTOEsperado = new ProjetoCardDTO();
        cardDTOEsperado.setId(id);
        cardDTOEsperado.setTitulo("Sistema de Gestão de Biblioteca V2");

        when(projetoRepository.findById(id)).thenReturn(Optional.of(projetoTeste));

        // 2. Mock do saveAndFlush em vez do save
        when(projetoRepository.saveAndFlush(any(Projeto.class))).thenReturn(projetoTeste);

        // 3. Mock do Mapper (garanta que a variável na classe de teste se chama
        // 'mapper' ou 'projetoMapper')
        when(mapper.toCardDTO(any(Projeto.class))).thenReturn(cardDTOEsperado);

        // Act
        ProjetoCardDTO resultado = projetoService.atualizarProjeto(id, dtoAtualizacao, "carlos@unifapi.edu.br");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(projetoTeste.getTitulo()).isEqualTo("Sistema de Gestão de Biblioteca V2");
        assertThat(projetoTeste.getVagas()).isEqualTo(5);

        verify(projetoRepository).findById(id);
        // 4. Verificação com saveAndFlush (atLeastOnce para cobrir as chamadas do
        // saveAndFlush)
        verify(projetoRepository, atLeastOnce()).saveAndFlush(any(Projeto.class));
        verify(mapper).toCardDTO(any(Projeto.class));
    }

    @Test
    @DisplayName("Deve falhar ao atualizar projeto quando o usuário não for o criador")
    void deveFalharAtualizarProjetoSemPermissao() {
        // Arrange
        UUID id = projetoTeste.getId();
        String emailOutroUsuario = "outro.professor@unifapi.edu.br";

        when(projetoRepository.findById(id)).thenReturn(Optional.of(projetoTeste));

        // Act & Assert
        assertThatThrownBy(() -> projetoService.atualizarProjeto(id, projetoRequestDTO, emailOutroUsuario))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Você não tem permissão para alterar este projeto.");

        verify(projetoRepository).findById(id);
        verify(projetoRepository, never()).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Deve falhar ao atualizar projeto inexistente")
    void deveFalharAtualizarProjetoInexistente() {
        // Arrange
        UUID idInexistente = UUID.randomUUID();
        when(projetoRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(
                () -> projetoService.atualizarProjeto(idInexistente, projetoRequestDTO, "carlos@unifapi.edu.br"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Projeto não encontrado");

        verify(projetoRepository).findById(idInexistente);
        verify(projetoRepository, never()).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Deve deletar projeto")
    void deveDeletarProjeto() {
        UUID id = projetoTeste.getId();
        doNothing().when(projetoRepository).deleteById(id);

        projetoService.delete(id);

        verify(projetoRepository).deleteById(id);
    }

    @Test
    @DisplayName("Deve listar projetos por criador")
    void deveListarProjetosPorCriador() {
        UUID criadorId = criadorTeste.getId();
        when(projetoRepository.findByCriadorId(criadorId))
                .thenReturn(Arrays.asList(projetoTeste));

        var resultado = projetoService.findByCriador(criadorId);

        assertThat(resultado).isNotEmpty().hasSize(1).contains(projetoTeste);
        verify(projetoRepository).findByCriadorId(criadorId);
    }

    @Test
    @DisplayName("Deve listar projetos por status com paginação")
    void deveListarProjetosPorStatus() {
        Page<Projeto> pageProjetos = new PageImpl<>(
                Arrays.asList(projetoTeste),
                Pageable.ofSize(10),
                1);

        when(projetoRepository.findByStatus(StatusProjeto.ABERTO, Pageable.ofSize(10)))
                .thenReturn(pageProjetos);

        ProjetoPageResponseDTO resultado = projetoService.findByStatusWithDetails(
                StatusProjeto.ABERTO,
                Pageable.ofSize(10));

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDados()).isNotEmpty();

        verify(projetoRepository).findByStatus(StatusProjeto.ABERTO, Pageable.ofSize(10));
    }

    @Test
    @DisplayName("Deve decrementar vagas ao aceitar candidato")
    void deveDecrementarVagas() {
        UUID id = projetoTeste.getId();
        projetoTeste.setVagasPreenchidas(0);
        projetoTeste.setVagas(3);

        when(projetoRepository.findById(id)).thenReturn(Optional.of(projetoTeste));
        when(projetoRepository.save(any(Projeto.class))).thenReturn(projetoTeste);

        projetoService.decrementarVagas(id);

        assertThat(projetoTeste.getVagasPreenchidas()).isEqualTo(1);
        verify(projetoRepository).findById(id);
        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    @DisplayName("Não deve decrementar vagas se todas preenchidas")
    void naoDeveDecrementarVagasSeTodasPreenchidas() {
        UUID id = projetoTeste.getId();
        projetoTeste.setVagasPreenchidas(3);
        projetoTeste.setVagas(3);

        when(projetoRepository.findById(id)).thenReturn(Optional.of(projetoTeste));

        projetoService.decrementarVagas(id);

        assertThat(projetoTeste.getVagasPreenchidas()).isEqualTo(3);
        verify(projetoRepository).findById(id);
        verify(projetoRepository, never()).save(any(Projeto.class));
    }
}