package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Avaliacao;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.avaliacao.AvaliacaoResponse;
import com.unifapi.academico.dto.avaliacao.AvaliarRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.AvaliacaoRepository;
import com.unifapi.academico.repository.ProjetoMembroRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.unifapi.academico.support.EntityFixtures.avaliacao;
import static com.unifapi.academico.support.EntityFixtures.nivelAcesso;
import static com.unifapi.academico.support.EntityFixtures.projeto;
import static com.unifapi.academico.support.EntityFixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (Mockito puro, sem contexto Spring) para {@link AvaliacaoService}.
 */
@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private ProjetoMembroRepository projetoMembroRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private AvaliacaoService service;

    private NivelAcesso nivelAcesso;
    private Usuario criador;
    private Usuario membro;
    private Usuario estranho;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        service = new AvaliacaoService(avaliacaoRepository, projetoRepository, projetoMembroRepository, usuarioRepository);

        nivelAcesso = nivelAcesso("ALUNO");
        criador = usuario("criador@teste.com", nivelAcesso);
        membro = usuario("membro@teste.com", nivelAcesso);
        estranho = usuario("estranho@teste.com", nivelAcesso);
        projeto = projeto(criador, "Projeto de teste", 3, StatusProjeto.CONCLUIDO);
    }

    // ============================================
    // avaliar()
    // ============================================

    @Test
    void deveLancarExcecaoAoTentarAvaliarASiMesmo() {
        AvaliarRequest request = new AvaliarRequest(criador.getId(), 5, "Ótimo");

        assertThatThrownBy(() -> service.avaliar(projeto.getId(), request, criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não pode avaliar a si mesmo");

        verify(projetoRepository, never()).findById(any());
        verifyNoMoreInteractions(projetoRepository, projetoMembroRepository, usuarioRepository, avaliacaoRepository);
    }

    @Test
    void deveLancarExcecaoQuandoProjetoNaoEncontrado() {
        UUID projetoId = UUID.randomUUID();
        AvaliarRequest request = new AvaliarRequest(membro.getId(), 5, "Ótimo");
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.avaliar(projetoId, request, criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Projeto não encontrado");

        verify(usuarioRepository, never()).findById(any());
        verify(avaliacaoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "status {0} não permite avaliação")
    @EnumSource(value = StatusProjeto.class, names = {"ABERTO", "EM_ANDAMENTO"})
    void deveLancarExcecaoQuandoProjetoNaoEstaConcluidoOuCancelado(StatusProjeto status) {
        Projeto projetoEmAndamento = projeto(criador, "Projeto em curso", 3, status);
        AvaliarRequest request = new AvaliarRequest(membro.getId(), 5, "Ótimo");
        when(projetoRepository.findById(projetoEmAndamento.getId())).thenReturn(Optional.of(projetoEmAndamento));

        assertThatThrownBy(() -> service.avaliar(projetoEmAndamento.getId(), request, criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("concluído ou cancelado");

        verify(usuarioRepository, never()).findById(any());
        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoAvaliadorNaoParticipouDoProjeto() {
        AvaliarRequest request = new AvaliarRequest(membro.getId(), 5, "Ótimo");
        when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), estranho.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.avaliar(projeto.getId(), request, estranho.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não participou deste projeto");

        verify(usuarioRepository, never()).findById(any());
        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoAvaliadoNaoEncontrado() {
        UUID avaliadoInexistenteId = UUID.randomUUID();
        AvaliarRequest request = new AvaliarRequest(avaliadoInexistenteId, 5, "Ótimo");
        when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
        when(usuarioRepository.findById(avaliadoInexistenteId)).thenReturn(Optional.empty());

        // avaliador é o criador -> participou sem precisar consultar ProjetoMembroRepository
        assertThatThrownBy(() -> service.avaliar(projeto.getId(), request, criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Usuário avaliado não encontrado");

        verify(projetoMembroRepository, never()).existsByProjeto_IdAndUsuario_Id(any(), any());
        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoAvaliadoNaoParticipouDoProjeto() {
        AvaliarRequest request = new AvaliarRequest(estranho.getId(), 5, "Ótimo");
        when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
        when(usuarioRepository.findById(estranho.getId())).thenReturn(Optional.of(estranho));
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), estranho.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.avaliar(projeto.getId(), request, criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Esta pessoa não participou deste projeto");

        verify(avaliacaoRepository, never())
                .existsByProjeto_IdAndAvaliador_IdAndAvaliado_IdAndRemovidaFalse(any(), any(), any());
        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoJaExisteAvaliacaoNaoRemovidaParaOMesmoTrio() {
        AvaliarRequest request = new AvaliarRequest(membro.getId(), 5, "Ótimo");
        when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
        when(usuarioRepository.findById(membro.getId())).thenReturn(Optional.of(membro));
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), membro.getId()))
                .thenReturn(true);
        when(avaliacaoRepository.existsByProjeto_IdAndAvaliador_IdAndAvaliado_IdAndRemovidaFalse(
                projeto.getId(), criador.getId(), membro.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.avaliar(projeto.getId(), request, criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já avaliou esta pessoa");

        verify(avaliacaoRepository, never()).save(any());
        verify(usuarioRepository, never()).getReferenceById(any());
    }

    @Test
    void devePermitirNovaAvaliacaoQuandoAAnteriorFoiRemovidaPorDenuncia() {
        AvaliarRequest request = new AvaliarRequest(membro.getId(), 4, "Trabalho recuperado após denúncia anterior");
        when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
        when(usuarioRepository.findById(membro.getId())).thenReturn(Optional.of(membro));
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), membro.getId()))
                .thenReturn(true);
        // Existe uma avaliação anterior, mas está removida (denunciada) -> exists...RemovidaFalse é false.
        when(avaliacaoRepository.existsByProjeto_IdAndAvaliador_IdAndAvaliado_IdAndRemovidaFalse(
                projeto.getId(), criador.getId(), membro.getId())).thenReturn(false);
        when(usuarioRepository.getReferenceById(criador.getId())).thenReturn(criador);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        AvaliacaoResponse response = service.avaliar(projeto.getId(), request, criador.getId());

        assertThat(response).isNotNull();
        assertThat(response.nota()).isEqualTo(4);
        verify(avaliacaoRepository).save(any(Avaliacao.class));
    }

    @Test
    void deveNormalizarComentarioEmBrancoParaNuloAoSalvar() {
        AvaliarRequest request = new AvaliarRequest(membro.getId(), 5, "   ");
        configurarCenarioDeSucesso(criador, membro);

        service.avaliar(projeto.getId(), request, criador.getId());

        ArgumentCaptor<Avaliacao> captor = ArgumentCaptor.forClass(Avaliacao.class);
        verify(avaliacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getComentario()).isNull();
    }

    @Test
    void deveManterComentarioValidoAoSalvar() {
        AvaliarRequest request = new AvaliarRequest(membro.getId(), 5, "  Excelente colaborador  ");
        configurarCenarioDeSucesso(criador, membro);

        service.avaliar(projeto.getId(), request, criador.getId());

        ArgumentCaptor<Avaliacao> captor = ArgumentCaptor.forClass(Avaliacao.class);
        verify(avaliacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getComentario()).isEqualTo("Excelente colaborador");
    }

    @Test
    void devePermitirCriadorAvaliarMembroQuandoProjetoConcluido() {
        AvaliarRequest request = new AvaliarRequest(membro.getId(), 5, "Ótimo trabalho em equipe");
        configurarCenarioDeSucesso(criador, membro);

        AvaliacaoResponse response = service.avaliar(projeto.getId(), request, criador.getId());

        assertThat(response.nota()).isEqualTo(5);
        assertThat(response.avaliador().id()).isEqualTo(criador.getId());
        // avaliador é o criador: participouDoProjeto não precisa consultar ProjetoMembroRepository para ele.
        verify(projetoMembroRepository, never()).existsByProjeto_IdAndUsuario_Id(projeto.getId(), criador.getId());
    }

    @Test
    void devePermitirMembroAvaliarCriadorQuandoProjetoCancelado() {
        Projeto projetoCancelado = projeto(criador, "Projeto cancelado", 3, StatusProjeto.CANCELADO);
        AvaliarRequest request = new AvaliarRequest(criador.getId(), 3, "Faltou comunicação");

        when(projetoRepository.findById(projetoCancelado.getId())).thenReturn(Optional.of(projetoCancelado));
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projetoCancelado.getId(), membro.getId()))
                .thenReturn(true);
        when(usuarioRepository.findById(criador.getId())).thenReturn(Optional.of(criador));
        when(avaliacaoRepository.existsByProjeto_IdAndAvaliador_IdAndAvaliado_IdAndRemovidaFalse(
                projetoCancelado.getId(), membro.getId(), criador.getId())).thenReturn(false);
        when(usuarioRepository.getReferenceById(membro.getId())).thenReturn(membro);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        AvaliacaoResponse response = service.avaliar(projetoCancelado.getId(), request, membro.getId());

        assertThat(response.nota()).isEqualTo(3);
        assertThat(response.avaliador().id()).isEqualTo(membro.getId());
        // avaliado é o criador do projeto: participouDoProjeto não precisa da consulta ao ProjetoMembroRepository.
        verify(projetoMembroRepository, never()).existsByProjeto_IdAndUsuario_Id(projetoCancelado.getId(), criador.getId());
    }

    @Test
    void deveDispararRecalculoDeMediaDoAvaliadoAoSalvarComSucesso() {
        AvaliarRequest request = new AvaliarRequest(membro.getId(), 5, "Muito bom");
        configurarCenarioDeSucesso(criador, membro);
        when(avaliacaoRepository.calcularMediaPorAvaliado(membro.getId())).thenReturn(4.5);
        when(avaliacaoRepository.countByAvaliado_IdAndRemovidaFalse(membro.getId())).thenReturn(2L);

        service.avaliar(projeto.getId(), request, criador.getId());

        verify(avaliacaoRepository).flush();
        // findById do avaliado é chamado duas vezes: validação de participação e recalcularMedia.
        verify(usuarioRepository, times(2)).findById(membro.getId());
        verify(avaliacaoRepository).calcularMediaPorAvaliado(membro.getId());
        verify(avaliacaoRepository).countByAvaliado_IdAndRemovidaFalse(membro.getId());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getNotaMedia()).isEqualTo(4.5);
        assertThat(captor.getValue().getTotalAvaliacoes()).isEqualTo(2);
    }

    /** Configura o cenário feliz padrão: criador avalia membro, projeto CONCLUIDO, sem avaliação prévia. */
    private void configurarCenarioDeSucesso(Usuario avaliador, Usuario avaliado) {
        when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
        when(usuarioRepository.findById(avaliado.getId())).thenReturn(Optional.of(avaliado));
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), avaliado.getId()))
                .thenReturn(true);
        when(avaliacaoRepository.existsByProjeto_IdAndAvaliador_IdAndAvaliado_IdAndRemovidaFalse(
                projeto.getId(), avaliador.getId(), avaliado.getId())).thenReturn(false);
        when(usuarioRepository.getReferenceById(avaliador.getId())).thenReturn(avaliador);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ============================================
    // listarPorAvaliado()
    // ============================================

    @Test
    void deveListarAvaliacoesDoAvaliadoFiltrandoRemovidasEOrdenandoPorDataDesc() {
        UUID avaliadoId = membro.getId();
        Pageable pageable = PageRequest.of(0, 10);
        Avaliacao avaliacaoEntity = avaliacao(projeto, criador, membro, 5);
        Page<Avaliacao> pagina = new PageImpl<>(List.of(avaliacaoEntity), pageable, 1);

        when(avaliacaoRepository.findByAvaliado_IdAndRemovidaFalseOrderByCriadoEmDesc(avaliadoId, pageable))
                .thenReturn(pagina);

        Page<AvaliacaoResponse> resultado = service.listarPorAvaliado(avaliadoId, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nota()).isEqualTo(5);
        // O próprio nome do método do repositório já garante o filtro "RemovidaFalse" e a
        // ordenação "OrderByCriadoEmDesc"; o teste comprova que é exatamente esse método que é usado.
        verify(avaliacaoRepository).findByAvaliado_IdAndRemovidaFalseOrderByCriadoEmDesc(avaliadoId, pageable);
    }

    // ============================================
    // recalcularMedia() — pacote-privado, também chamado por DenunciaService
    // ============================================

    @Test
    void deveAtualizarNotaMediaETotalDeAvaliacoesQuandoExistemAvaliacoesValidas() {
        UUID usuarioId = membro.getId();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(membro));
        when(avaliacaoRepository.calcularMediaPorAvaliado(usuarioId)).thenReturn(4.2);
        when(avaliacaoRepository.countByAvaliado_IdAndRemovidaFalse(usuarioId)).thenReturn(5L);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recalcularMedia(usuarioId);

        assertThat(membro.getNotaMedia()).isEqualTo(4.2);
        assertThat(membro.getTotalAvaliacoes()).isEqualTo(5);
        verify(usuarioRepository).save(membro);
    }

    @Test
    void deveDefinirNotaMediaComoNulaQuandoNaoHaAvaliacoesNaoRemovidas() {
        UUID usuarioId = membro.getId();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(membro));
        when(avaliacaoRepository.calcularMediaPorAvaliado(usuarioId)).thenReturn(null);
        when(avaliacaoRepository.countByAvaliado_IdAndRemovidaFalse(usuarioId)).thenReturn(0L);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recalcularMedia(usuarioId);

        assertThat(membro.getNotaMedia()).isNull();
        assertThat(membro.getTotalAvaliacoes()).isZero();
        verify(usuarioRepository).save(membro);
    }

    @Test
    void naoDeveLancarExcecaoQuandoUsuarioNaoEncontradoAoRecalcularMedia() {
        UUID usuarioIdInexistente = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioIdInexistente)).thenReturn(Optional.empty());

        service.recalcularMedia(usuarioIdInexistente);

        verify(avaliacaoRepository, never()).calcularMediaPorAvaliado(any());
        verify(avaliacaoRepository, never()).countByAvaliado_IdAndRemovidaFalse(any());
        verify(usuarioRepository, never()).save(any());
    }
}
