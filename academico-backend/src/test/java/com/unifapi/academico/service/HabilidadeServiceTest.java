package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.dto.habilidade.AtualizarHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.CriarHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.HabilidadeResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.HabilidadeRepository;
import com.unifapi.academico.repository.ProjetoHabilidadeRepository;
import com.unifapi.academico.repository.UsuarioHabilidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.unifapi.academico.support.EntityFixtures.habilidade;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (Mockito puro, sem contexto Spring) para {@link HabilidadeService}.
 */
@ExtendWith(MockitoExtension.class)
class HabilidadeServiceTest {

    @Mock
    private HabilidadeRepository habilidadeRepository;

    @Mock
    private ProjetoHabilidadeRepository projetoHabilidadeRepository;

    @Mock
    private UsuarioHabilidadeRepository usuarioHabilidadeRepository;

    private HabilidadeService service;

    private Habilidade habilidade;

    @BeforeEach
    void setUp() {
        service = new HabilidadeService(habilidadeRepository, projetoHabilidadeRepository, usuarioHabilidadeRepository);
        habilidade = habilidade("Java", "Backend");
    }

    // ============================================
    // criar()
    // ============================================

    @Test
    void deveCriarHabilidadeComNomeAparadoQuandoNaoExisteDuplicidade() {
        CriarHabilidadeRequest request = new CriarHabilidadeRequest("  Java  ", "Backend", "Linguagem de programação");
        when(habilidadeRepository.existsByNomeIgnoreCase("Java")).thenReturn(false);
        when(habilidadeRepository.save(any(Habilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        HabilidadeResponse response = service.criar(request);

        assertThat(response.nome()).isEqualTo("Java");
        ArgumentCaptor<Habilidade> captor = ArgumentCaptor.forClass(Habilidade.class);
        verify(habilidadeRepository).save(captor.capture());
        assertThat(captor.getValue().getNome()).isEqualTo("Java");
        assertThat(captor.getValue().getCategoria()).isEqualTo("Backend");
    }

    @Test
    void deveLancarExcecaoAoCriarHabilidadeComNomeJaExistenteIgnorandoCaixa() {
        CriarHabilidadeRequest request = new CriarHabilidadeRequest("JAVA", "Backend", null);
        when(habilidadeRepository.existsByNomeIgnoreCase("JAVA")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe uma habilidade cadastrada com o nome 'JAVA'");

        verify(habilidadeRepository, never()).save(any());
    }

    @Test
    void deveNormalizarCategoriaEDescricaoEmBrancoParaNuloAoCriar() {
        CriarHabilidadeRequest request = new CriarHabilidadeRequest("Kotlin", "   ", "   ");
        when(habilidadeRepository.existsByNomeIgnoreCase("Kotlin")).thenReturn(false);
        when(habilidadeRepository.save(any(Habilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        service.criar(request);

        ArgumentCaptor<Habilidade> captor = ArgumentCaptor.forClass(Habilidade.class);
        verify(habilidadeRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoria()).isNull();
        assertThat(captor.getValue().getDescricao()).isNull();
    }

    // ============================================
    // atualizar()
    // ============================================

    @Test
    void deveAtualizarHabilidadeQuandoNomeNaoConflitaComOutraExistente() {
        UUID id = habilidade.getId();
        AtualizarHabilidadeRequest request = new AtualizarHabilidadeRequest("Java Avançado", "Backend", "Descrição nova");
        when(habilidadeRepository.findById(id)).thenReturn(Optional.of(habilidade));
        when(habilidadeRepository.existsByNomeIgnoreCaseAndIdNot("Java Avançado", id)).thenReturn(false);
        when(habilidadeRepository.save(any(Habilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        HabilidadeResponse response = service.atualizar(id, request);

        assertThat(response.nome()).isEqualTo("Java Avançado");
        assertThat(habilidade.getCategoria()).isEqualTo("Backend");
        assertThat(habilidade.getDescricao()).isEqualTo("Descrição nova");
    }

    @Test
    void devePermitirRenomearParaOMesmoNomeAtualSemLancarExcecao() {
        UUID id = habilidade.getId();
        AtualizarHabilidadeRequest request = new AtualizarHabilidadeRequest("Java", "Backend", null);
        when(habilidadeRepository.findById(id)).thenReturn(Optional.of(habilidade));
        // Exclusão do próprio id: renomear para o nome já usado por si mesma não conflita.
        when(habilidadeRepository.existsByNomeIgnoreCaseAndIdNot("Java", id)).thenReturn(false);
        when(habilidadeRepository.save(any(Habilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        HabilidadeResponse response = service.atualizar(id, request);

        assertThat(response.nome()).isEqualTo("Java");
        verify(habilidadeRepository).save(any(Habilidade.class));
    }

    @Test
    void deveLancarExcecaoAoAtualizarParaNomeUsadoPorOutraHabilidade() {
        UUID id = habilidade.getId();
        AtualizarHabilidadeRequest request = new AtualizarHabilidadeRequest("Python", "Backend", null);
        when(habilidadeRepository.findById(id)).thenReturn(Optional.of(habilidade));
        when(habilidadeRepository.existsByNomeIgnoreCaseAndIdNot("Python", id)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe outra habilidade cadastrada com o nome 'Python'");

        verify(habilidadeRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoAtualizarHabilidadeInexistente() {
        UUID idInexistente = UUID.randomUUID();
        AtualizarHabilidadeRequest request = new AtualizarHabilidadeRequest("Java", "Backend", null);
        when(habilidadeRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(idInexistente, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(habilidadeRepository, never()).existsByNomeIgnoreCaseAndIdNot(any(), any());
        verify(habilidadeRepository, never()).save(any());
    }

    // ============================================
    // remover()
    // ============================================

    @Test
    void deveRemoverHabilidadeQuandoNaoEstaEmUsoPorProjetosOuUsuarios() {
        UUID id = habilidade.getId();
        when(habilidadeRepository.findById(id)).thenReturn(Optional.of(habilidade));
        when(projetoHabilidadeRepository.countByHabilidade_Id(id)).thenReturn(0L);
        when(usuarioHabilidadeRepository.countByHabilidade_Id(id)).thenReturn(0L);

        service.remover(id);

        verify(habilidadeRepository).delete(habilidade);
    }

    @Test
    void deveLancarExcecaoAoRemoverHabilidadeEmUsoPorProjetos() {
        UUID id = habilidade.getId();
        when(habilidadeRepository.findById(id)).thenReturn(Optional.of(habilidade));
        when(projetoHabilidadeRepository.countByHabilidade_Id(id)).thenReturn(2L);
        when(usuarioHabilidadeRepository.countByHabilidade_Id(id)).thenReturn(0L);

        assertThatThrownBy(() -> service.remover(id))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("está em uso");

        verify(habilidadeRepository, never()).delete(any());
    }

    @Test
    void deveLancarExcecaoAoRemoverHabilidadeEmUsoPorUsuarios() {
        UUID id = habilidade.getId();
        when(habilidadeRepository.findById(id)).thenReturn(Optional.of(habilidade));
        when(projetoHabilidadeRepository.countByHabilidade_Id(id)).thenReturn(0L);
        when(usuarioHabilidadeRepository.countByHabilidade_Id(id)).thenReturn(1L);

        assertThatThrownBy(() -> service.remover(id))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("está em uso");

        verify(habilidadeRepository, never()).delete(any());
    }

    @Test
    void deveLancarExcecaoAoRemoverHabilidadeInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(habilidadeRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remover(idInexistente))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(projetoHabilidadeRepository, never()).countByHabilidade_Id(any());
        verify(habilidadeRepository, never()).delete(any());
    }

    // ============================================
    // listar()
    // ============================================

    @Test
    void deveListarHabilidadesRepassandoBuscaCaseInsensitiveECategoriaExata() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Habilidade> pagina = new PageImpl<>(List.of(habilidade), pageable, 1);
        when(habilidadeRepository.buscar("java", "Backend", pageable)).thenReturn(pagina);

        Page<HabilidadeResponse> resultado = service.listar("  java  ", "  Backend  ", pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nome()).isEqualTo("Java");
        verify(habilidadeRepository).buscar("java", "Backend", pageable);
    }

    @Test
    void deveListarHabilidadesComBuscaECategoriaNulasQuandoParametrosEmBranco() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Habilidade> pagina = new PageImpl<>(List.of(), pageable, 0);
        when(habilidadeRepository.buscar(isNull(), isNull(), eq(pageable))).thenReturn(pagina);

        Page<HabilidadeResponse> resultado = service.listar("   ", null, pageable);

        assertThat(resultado.getTotalElements()).isZero();
        verify(habilidadeRepository).buscar(isNull(), isNull(), eq(pageable));
    }

    // ============================================
    // listarCategorias()
    // ============================================

    @Test
    void deveListarCategoriasDistintas() {
        when(habilidadeRepository.listarCategorias()).thenReturn(List.of("Backend", "Frontend"));

        List<String> categorias = service.listarCategorias();

        assertThat(categorias).containsExactly("Backend", "Frontend");
        verify(habilidadeRepository).listarCategorias();
    }

    // ============================================
    // buscarPorId()
    // ============================================

    @Test
    void deveBuscarHabilidadePorId() {
        when(habilidadeRepository.findById(habilidade.getId())).thenReturn(Optional.of(habilidade));

        HabilidadeResponse response = service.buscarPorId(habilidade.getId());

        assertThat(response.id()).isEqualTo(habilidade.getId());
    }

    @Test
    void deveLancarExcecaoAoBuscarPorIdInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(habilidadeRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(idInexistente))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ============================================
    // recalcularContadores()
    // ============================================

    @Test
    void deveRecalcularContadoresAPartirDasTabelasDeVinculo() {
        UUID id = habilidade.getId();
        when(habilidadeRepository.findById(id)).thenReturn(Optional.of(habilidade));
        when(projetoHabilidadeRepository.countByHabilidade_Id(id)).thenReturn(3L);
        when(usuarioHabilidadeRepository.countByHabilidade_Id(id)).thenReturn(5L);
        when(habilidadeRepository.save(any(Habilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recalcularContadores(id);

        assertThat(habilidade.getProjetosCount()).isEqualTo(3);
        assertThat(habilidade.getUsuariosCount()).isEqualTo(5);
        verify(habilidadeRepository).save(habilidade);
    }

    @Test
    void naoDeveLancarExcecaoAoRecalcularContadoresDeHabilidadeInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(habilidadeRepository.findById(idInexistente)).thenReturn(Optional.empty());

        service.recalcularContadores(idInexistente);

        verify(projetoHabilidadeRepository, never()).countByHabilidade_Id(any());
        verify(usuarioHabilidadeRepository, never()).countByHabilidade_Id(any());
        verify(habilidadeRepository, never()).save(any());
    }
}
