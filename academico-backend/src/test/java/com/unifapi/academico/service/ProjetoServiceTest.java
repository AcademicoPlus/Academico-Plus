package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.ProjetoCanceladoAviso;
import com.unifapi.academico.domain.models.ProjetoHabilidade;
import com.unifapi.academico.domain.models.ProjetoMembro;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.habilidade.VincularHabilidadeRequest;
import com.unifapi.academico.dto.projeto.AlterarStatusProjetoRequest;
import com.unifapi.academico.dto.projeto.AtualizarProjetoRequest;
import com.unifapi.academico.dto.projeto.CriarProjetoRequest;
import com.unifapi.academico.dto.projeto.ProjetoResponse;
import com.unifapi.academico.dto.projeto.ProjetoResumoResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CandidaturaRepository;
import com.unifapi.academico.repository.HabilidadeRepository;
import com.unifapi.academico.repository.ProjetoCanceladoAvisoRepository;
import com.unifapi.academico.repository.ProjetoHabilidadeRepository;
import com.unifapi.academico.repository.ProjetoMembroRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.support.EntityFixtures;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (Mockito puro, sem contexto Spring) para {@link ProjetoService}.
 */
@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    @Mock private ProjetoRepository projetoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProjetoMembroRepository projetoMembroRepository;
    @Mock private CandidaturaRepository candidaturaRepository;
    @Mock private ProjetoHabilidadeRepository projetoHabilidadeRepository;
    @Mock private HabilidadeRepository habilidadeRepository;
    @Mock private ProjetoCanceladoAvisoRepository projetoCanceladoAvisoRepository;
    @Mock private HabilidadeService habilidadeService;
    @Mock private BannerStorageClient bannerStorageClient;

    private ProjetoService projetoService;

    private Usuario criador;
    private Usuario outroUsuario;

    @BeforeEach
    void setUp() {
        projetoService = new ProjetoService(
                projetoRepository,
                usuarioRepository,
                projetoMembroRepository,
                candidaturaRepository,
                projetoHabilidadeRepository,
                habilidadeRepository,
                projetoCanceladoAvisoRepository,
                habilidadeService,
                bannerStorageClient);

        criador = EntityFixtures.usuario("criador@unifapi.edu.br", null);
        outroUsuario = EntityFixtures.usuario("outro@unifapi.edu.br", null);
    }

    // ============================================
    // HELPERS
    // ============================================

    private CriarProjetoRequest criarRequest(List<VincularHabilidadeRequest> habilidades) {
        return new CriarProjetoRequest("Projeto Teste", "Descricao com pelo menos vinte caracteres.", 5,
                LocalDateTime.now().plusDays(30), habilidades);
    }

    private AtualizarProjetoRequest atualizarRequest(int vagas, LocalDateTime dataFim) {
        return new AtualizarProjetoRequest("Titulo Atualizado", "Descricao atualizada com vinte caracteres.",
                vagas, dataFim);
    }

    private AlterarStatusProjetoRequest statusRequest(StatusProjeto status) {
        return new AlterarStatusProjetoRequest(status, null);
    }

    private AlterarStatusProjetoRequest statusRequest(StatusProjeto status, String motivo) {
        return new AlterarStatusProjetoRequest(status, motivo);
    }

    /** Stub das consultas usadas por responseDetalhado() para o projeto informado. */
    private void stubDetalhe(Projeto projeto, long totalMembros, long totalPendentes) {
        when(projetoRepository.findDetalhePorId(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(totalMembros);
        when(candidaturaRepository.countByProjeto_IdAndStatus(projeto.getId(), StatusCandidatura.PENDENTE))
                .thenReturn(totalPendentes);
    }

    // ============================================
    // criar()
    // ============================================

    @Test
    void criar_deveLancarRecursoNaoEncontrado_quandoCriadorNaoExiste() {
        UUID criadorId = UUID.randomUUID();
        when(usuarioRepository.findById(criadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projetoService.criar(criarRequest(null), criadorId))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void criar_deveDeduplicarHabilidadesRepetidas_eRecalcularContadoresUmaVezPorHabilidade() {
        UUID criadorId = criador.getId();
        Habilidade java = EntityFixtures.habilidade("Java");
        Projeto salvo = EntityFixtures.projeto(criador, 5);

        VincularHabilidadeRequest primeiro = new VincularHabilidadeRequest(java.getId(), true);
        VincularHabilidadeRequest duplicado = new VincularHabilidadeRequest(java.getId(), false);

        when(usuarioRepository.findById(criadorId)).thenReturn(Optional.of(criador));
        when(projetoRepository.save(any(Projeto.class))).thenReturn(salvo);
        when(habilidadeRepository.findById(java.getId())).thenReturn(Optional.of(java));
        when(projetoHabilidadeRepository.save(any(ProjetoHabilidade.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        stubDetalhe(salvo, 0, 0);

        projetoService.criar(criarRequest(List.of(primeiro, duplicado)), criadorId);

        verify(projetoHabilidadeRepository, times(1)).save(any(ProjetoHabilidade.class));
        verify(habilidadeService, times(1)).recalcularContadores(java.getId());
    }

    @Test
    void criar_deveLancarRecursoNaoEncontrado_eNaoRecalcularContadores_quandoHabilidadeNaoExiste() {
        UUID criadorId = criador.getId();
        UUID habilidadeInexistenteId = UUID.randomUUID();
        Projeto salvo = EntityFixtures.projeto(criador, 5);

        when(usuarioRepository.findById(criadorId)).thenReturn(Optional.of(criador));
        when(projetoRepository.save(any(Projeto.class))).thenReturn(salvo);
        when(habilidadeRepository.findById(habilidadeInexistenteId)).thenReturn(Optional.empty());

        VincularHabilidadeRequest pedido = new VincularHabilidadeRequest(habilidadeInexistenteId, null);

        assertThatThrownBy(() -> projetoService.criar(criarRequest(List.of(pedido)), criadorId))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(projetoHabilidadeRepository, never()).save(any());
        verify(habilidadeService, never()).recalcularContadores(any());
    }

    @Test
    void criar_deveDefinirObrigatoriaComoFalse_quandoCampoForNulo() {
        UUID criadorId = criador.getId();
        Habilidade java = EntityFixtures.habilidade("Java");
        Projeto salvo = EntityFixtures.projeto(criador, 5);

        when(usuarioRepository.findById(criadorId)).thenReturn(Optional.of(criador));
        when(projetoRepository.save(any(Projeto.class))).thenReturn(salvo);
        when(habilidadeRepository.findById(java.getId())).thenReturn(Optional.of(java));
        when(projetoHabilidadeRepository.save(any(ProjetoHabilidade.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        stubDetalhe(salvo, 0, 0);

        VincularHabilidadeRequest pedido = new VincularHabilidadeRequest(java.getId(), null);

        projetoService.criar(criarRequest(List.of(pedido)), criadorId);

        ArgumentCaptor<ProjetoHabilidade> captor = ArgumentCaptor.forClass(ProjetoHabilidade.class);
        verify(projetoHabilidadeRepository).save(captor.capture());
        assertThat(captor.getValue().getObrigatoria()).isFalse();
    }

    @Test
    void criar_deveSalvarProjetoComStatusAbertoEVagasPreenchidasZero() {
        UUID criadorId = criador.getId();
        Projeto salvo = EntityFixtures.projeto(criador, 5);

        when(usuarioRepository.findById(criadorId)).thenReturn(Optional.of(criador));
        when(projetoRepository.save(any(Projeto.class))).thenReturn(salvo);
        stubDetalhe(salvo, 0, 0);

        LocalDateTime dataFim = LocalDateTime.now().plusDays(15);
        CriarProjetoRequest request = new CriarProjetoRequest("  Projeto com espaços  ",
                "  Descricao com pelo menos vinte caracteres.  ", 7, dataFim, null);

        projetoService.criar(request, criadorId);

        ArgumentCaptor<Projeto> captor = ArgumentCaptor.forClass(Projeto.class);
        verify(projetoRepository).save(captor.capture());
        Projeto construido = captor.getValue();

        assertThat(construido.getTitulo()).isEqualTo("Projeto com espaços");
        assertThat(construido.getDescricao()).isEqualTo("Descricao com pelo menos vinte caracteres.");
        assertThat(construido.getVagas()).isEqualTo(7);
        assertThat(construido.getVagasPreenchidas()).isZero();
        assertThat(construido.getStatus()).isEqualTo(StatusProjeto.ABERTO);
        assertThat(construido.getAtivo()).isTrue();
        assertThat(construido.getDataFim()).isEqualTo(dataFim);
        assertThat(construido.getCriador()).isEqualTo(criador);
    }

    // ============================================
    // atualizar()
    // ============================================

    @Test
    void atualizar_devePropagarExcecao_quandoProjetoNaoEncontradoOuInativo() {
        UUID id = UUID.randomUUID();
        when(projetoRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projetoService.atualizar(id, atualizarRequest(5, null), criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizar_deveLancarRegraDeNegocio_quandoUsuarioNaoForCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() ->
                projetoService.atualizar(projeto.getId(), atualizarRequest(5, null), outroUsuario.getId()))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void atualizar_deveLancarRegraDeNegocio_quandoStatusForConcluido() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.CONCLUIDO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() ->
                projetoService.atualizar(projeto.getId(), atualizarRequest(5, null), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não podem ser editados");
    }

    @Test
    void atualizar_deveLancarRegraDeNegocio_quandoStatusForCancelado() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.CANCELADO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() ->
                projetoService.atualizar(projeto.getId(), atualizarRequest(5, null), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não podem ser editados");
    }

    @Test
    void atualizar_deveLancarRegraDeNegocio_quandoVagasMenorQueMembrosAtuais() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(3L);

        assertThatThrownBy(() ->
                projetoService.atualizar(projeto.getId(), atualizarRequest(2, null), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("vagas não pode ser menor");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void atualizar_devePermitir_quandoVagasIgualAMembrosAtuais() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(3L);
        stubDetalhe(projeto, 3, 0);

        projetoService.atualizar(projeto.getId(), atualizarRequest(3, null), criador.getId());

        verify(projetoRepository).save(projeto);
        assertThat(projeto.getVagas()).isEqualTo(3);
    }

    @Test
    void atualizar_deveLancarRegraDeNegocio_quandoDataFimAlteradaParaPassado() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        projeto.setDataFim(null);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(0L);

        LocalDateTime dataPassada = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() ->
                projetoService.atualizar(projeto.getId(), atualizarRequest(5, dataPassada), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("data futura");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void atualizar_naoDeveLancarExcecao_quandoDataFimInalteradaMesmoQuePassada() {
        LocalDateTime dataJaVencida = LocalDateTime.now().minusDays(5);
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        projeto.setDataFim(dataJaVencida);

        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(0L);
        stubDetalhe(projeto, 0, 0);

        // Resubmete exatamente a mesma data já vencida: dataMudou é falso, não deve lançar.
        projetoService.atualizar(projeto.getId(), atualizarRequest(5, dataJaVencida), criador.getId());

        verify(projetoRepository).save(projeto);
        assertThat(projeto.getDataFim()).isEqualTo(dataJaVencida);
    }

    @Test
    void atualizar_deveAtualizarCamposEData_quandoDataFimAlteradaParaFuturo() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        projeto.setDataFim(LocalDateTime.now().plusDays(1));

        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(0L);
        stubDetalhe(projeto, 0, 0);

        LocalDateTime novaData = LocalDateTime.now().plusDays(60);
        projetoService.atualizar(projeto.getId(), atualizarRequest(9, novaData), criador.getId());

        assertThat(projeto.getTitulo()).isEqualTo("Titulo Atualizado");
        assertThat(projeto.getDescricao()).isEqualTo("Descricao atualizada com vinte caracteres.");
        assertThat(projeto.getVagas()).isEqualTo(9);
        assertThat(projeto.getDataFim()).isEqualTo(novaData);
    }

    // ============================================
    // alterarStatus()
    // ============================================

    @Test
    void alterarStatus_devePropagarExcecao_quandoUsuarioNaoForCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.alterarStatus(projeto.getId(),
                statusRequest(StatusProjeto.EM_ANDAMENTO), outroUsuario.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void alterarStatus_deveLancarRegraDeNegocio_quandoStatusIgualAoAtual() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.ABERTO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.alterarStatus(projeto.getId(),
                statusRequest(StatusProjeto.ABERTO), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já está com o status");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void alterarStatus_deveLancarRegraDeNegocio_quandoAtualForConcluido() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.CONCLUIDO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.alterarStatus(projeto.getId(),
                statusRequest(StatusProjeto.ABERTO), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não podem ter o status alterado");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void alterarStatus_deveLancarRegraDeNegocio_quandoAtualForCancelado() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.CANCELADO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.alterarStatus(projeto.getId(),
                statusRequest(StatusProjeto.EM_ANDAMENTO), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não podem ter o status alterado");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void alterarStatus_devePermitirTransicaoDiretaAbertoParaConcluido() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.ABERTO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        stubDetalhe(projeto, 0, 0);

        ProjetoResponse response = projetoService.alterarStatus(projeto.getId(),
                statusRequest(StatusProjeto.CONCLUIDO), criador.getId());

        assertThat(projeto.getStatus()).isEqualTo(StatusProjeto.CONCLUIDO);
        assertThat(response.status()).isEqualTo(StatusProjeto.CONCLUIDO);
        verify(projetoRepository).save(projeto);
        verify(projetoCanceladoAvisoRepository, never()).saveAll(anyList());
    }

    @Test
    void alterarStatus_devePermitirTransicaoRetroativaEmAndamentoParaAberto() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.EM_ANDAMENTO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        stubDetalhe(projeto, 0, 0);

        projetoService.alterarStatus(projeto.getId(), statusRequest(StatusProjeto.ABERTO), criador.getId());

        assertThat(projeto.getStatus()).isEqualTo(StatusProjeto.ABERTO);
        verify(projetoCanceladoAvisoRepository, never()).saveAll(anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void alterarStatus_deveRegistrarAvisoParaCadaMembro_quandoTransicionarParaCancelado_eNaoParaOCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto Cancelável", 5, StatusProjeto.ABERTO);
        Usuario membro1 = EntityFixtures.usuario("membro1@unifapi.edu.br", null);
        Usuario membro2 = EntityFixtures.usuario("membro2@unifapi.edu.br", null);
        List<ProjetoMembro> membros = List.of(
                EntityFixtures.projetoMembro(projeto, membro1, "Dev"),
                EntityFixtures.projetoMembro(projeto, membro2, "Dev"));

        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.findByProjeto_IdOrderByDataAdesaoAsc(projeto.getId())).thenReturn(membros);
        stubDetalhe(projeto, 2, 0);

        projetoService.alterarStatus(projeto.getId(), statusRequest(StatusProjeto.CANCELADO), criador.getId());

        ArgumentCaptor<List<ProjetoCanceladoAviso>> captor = ArgumentCaptor.forClass(List.class);
        verify(projetoCanceladoAvisoRepository, times(1)).saveAll(captor.capture());

        List<ProjetoCanceladoAviso> avisos = captor.getValue();
        assertThat(avisos).hasSize(2);
        assertThat(avisos).extracting(ProjetoCanceladoAviso::getUsuario)
                .containsExactlyInAnyOrder(membro1, membro2)
                .doesNotContain(criador);
        assertThat(avisos).allSatisfy(aviso -> {
            assertThat(aviso.getTipo()).isEqualTo("cancelamento");
            assertThat(aviso.getProjetoNome()).isEqualTo(projeto.getTitulo());
            assertThat(aviso.getProjeto()).isEqualTo(projeto);
        });
    }

    @Test
    void alterarStatus_naoDeveRegistrarAvisos_quandoNovoStatusNaoForCancelado() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.ABERTO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        stubDetalhe(projeto, 0, 0);

        projetoService.alterarStatus(projeto.getId(), statusRequest(StatusProjeto.EM_ANDAMENTO), criador.getId());

        verify(projetoMembroRepository, never()).findByProjeto_IdOrderByDataAdesaoAsc(any());
        verify(projetoCanceladoAvisoRepository, never()).saveAll(anyList());
    }

    @Test
    void alterarStatus_naoDeveRegistrarAvisos_quandoProjetoNaoTiverMembros() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.ABERTO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.findByProjeto_IdOrderByDataAdesaoAsc(projeto.getId()))
                .thenReturn(List.of());
        stubDetalhe(projeto, 0, 0);

        projetoService.alterarStatus(projeto.getId(), statusRequest(StatusProjeto.CANCELADO), criador.getId());

        verify(projetoCanceladoAvisoRepository, never()).saveAll(anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void alterarStatus_motivoNaoDeveAlterarConteudoDosAvisos() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto com motivo", 5, StatusProjeto.ABERTO);
        Usuario membro1 = EntityFixtures.usuario("membroX@unifapi.edu.br", null);
        List<ProjetoMembro> membros = List.of(EntityFixtures.projetoMembro(projeto, membro1, "Dev"));

        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.findByProjeto_IdOrderByDataAdesaoAsc(projeto.getId())).thenReturn(membros);
        stubDetalhe(projeto, 1, 0);

        // O DTO aceita motivo, mas o service atual não o persiste em nenhum lugar:
        // o conteúdo do aviso deve depender só dos dados do próprio projeto.
        projetoService.alterarStatus(projeto.getId(),
                statusRequest(StatusProjeto.CANCELADO, "Falta de patrocínio"), criador.getId());

        ArgumentCaptor<List<ProjetoCanceladoAviso>> captor = ArgumentCaptor.forClass(List.class);
        verify(projetoCanceladoAvisoRepository).saveAll(captor.capture());

        ProjetoCanceladoAviso aviso = captor.getValue().get(0);
        assertThat(aviso.getProjetoNome()).isEqualTo("Projeto com motivo");
        assertThat(aviso.getTipo()).isEqualTo("cancelamento");
        // ProjetoCanceladoAviso não possui campo "motivo": nada relacionado ao DTO é gravado.
    }

    // ============================================
    // inativar()
    // ============================================

    @Test
    void inativar_deveRegistrarAvisosEForcarCancelado_quandoProjetoNaoEstavaConcluidoOuCancelado() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.ABERTO);
        Usuario membro1 = EntityFixtures.usuario("membroA@unifapi.edu.br", null);
        List<ProjetoMembro> membros = List.of(EntityFixtures.projetoMembro(projeto, membro1, "Dev"));

        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.findByProjeto_IdOrderByDataAdesaoAsc(projeto.getId())).thenReturn(membros);

        projetoService.inativar(projeto.getId(), criador.getId());

        verify(projetoCanceladoAvisoRepository, times(1)).saveAll(anyList());
        assertThat(projeto.getStatus()).isEqualTo(StatusProjeto.CANCELADO);
        assertThat(projeto.getAtivo()).isFalse();
        verify(projetoRepository).save(projeto);
    }

    @Test
    void inativar_naoDeveRegistrarAvisos_quandoJaConcluido() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.CONCLUIDO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        projetoService.inativar(projeto.getId(), criador.getId());

        verify(projetoMembroRepository, never()).findByProjeto_IdOrderByDataAdesaoAsc(any());
        verify(projetoCanceladoAvisoRepository, never()).saveAll(anyList());
        assertThat(projeto.getStatus()).isEqualTo(StatusProjeto.CONCLUIDO);
        assertThat(projeto.getAtivo()).isFalse();
    }

    @Test
    void inativar_naoDeveRegistrarAvisos_quandoJaCancelado() {
        Projeto projeto = EntityFixtures.projeto(criador, "Projeto", 5, StatusProjeto.CANCELADO);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        projetoService.inativar(projeto.getId(), criador.getId());

        verify(projetoMembroRepository, never()).findByProjeto_IdOrderByDataAdesaoAsc(any());
        verify(projetoCanceladoAvisoRepository, never()).saveAll(anyList());
        assertThat(projeto.getStatus()).isEqualTo(StatusProjeto.CANCELADO);
        assertThat(projeto.getAtivo()).isFalse();
    }

    @Test
    void inativar_devePropagarExcecao_quandoUsuarioNaoForCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.inativar(projeto.getId(), outroUsuario.getId()))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(projetoRepository, never()).save(any());
    }

    // ============================================
    // sincronizarVagasPreenchidas()
    // ============================================

    @Test
    void sincronizarVagasPreenchidas_deveSobrescreverValorComContagemReal() {
        Projeto projeto = EntityFixtures.projeto(criador, 10);
        projeto.setVagasPreenchidas(99); // valor divergente/obsoleto propositalmente

        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(4L);

        projetoService.sincronizarVagasPreenchidas(projeto);

        assertThat(projeto.getVagasPreenchidas()).isEqualTo(4);
        verify(projetoRepository).save(projeto);
    }

    // ============================================
    // Leitura
    // ============================================

    @Test
    void listar_deveNormalizarBuscaEDelegarAoRepositorio() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Projeto> pagina = new PageImpl<>(List.of(projeto));

        when(projetoRepository.buscarAtivos(eq(StatusProjeto.ABERTO), eq("spring boot"), eq(pageable)))
                .thenReturn(pagina);

        Page<ProjetoResumoResponse> resultado =
                projetoService.listar(StatusProjeto.ABERTO, "  spring boot  ", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).titulo()).isEqualTo(projeto.getTitulo());
        verify(projetoRepository).buscarAtivos(StatusProjeto.ABERTO, "spring boot", pageable);
    }

    @Test
    void listar_deveConverterBuscaEmBrancoParaNulo() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projetoRepository.buscarAtivos(eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        projetoService.listar(null, "   ", pageable);

        verify(projetoRepository).buscarAtivos(null, null, pageable);
    }

    @Test
    void listarPorCriador_deveDelegarAoRepositorioComFiltroDeAtivos() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        Pageable pageable = PageRequest.of(0, 10);
        when(projetoRepository.findByCriador_IdAndAtivoTrue(criador.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(projeto)));

        Page<ProjetoResumoResponse> resultado = projetoService.listarPorCriador(criador.getId(), pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(projetoRepository).findByCriador_IdAndAtivoTrue(criador.getId(), pageable);
    }

    @Test
    void listarParticipando_deveDelegarAoRepositorioComFiltroDeAtivos() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        Pageable pageable = PageRequest.of(0, 10);
        when(projetoRepository.findByMembros_Usuario_IdAndAtivoTrue(outroUsuario.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(projeto)));

        Page<ProjetoResumoResponse> resultado = projetoService.listarParticipando(outroUsuario.getId(), pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(projetoRepository).findByMembros_Usuario_IdAndAtivoTrue(outroUsuario.getId(), pageable);
    }

    @Test
    void listarVinculados_semStatus_deveUsarConsultaSemFiltro() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        Pageable pageable = PageRequest.of(0, 10);
        when(projetoRepository.buscarVinculados(outroUsuario.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(projeto)));

        Page<ProjetoResumoResponse> resultado = projetoService.listarVinculados(outroUsuario.getId(), null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(projetoRepository, never()).buscarVinculadosPorStatus(any(), any(), any());
    }

    @Test
    void listarVinculados_comStatus_deveUsarConsultaFiltrada() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projetoRepository.buscarVinculadosPorStatus(outroUsuario.getId(), StatusProjeto.ABERTO, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        projetoService.listarVinculados(outroUsuario.getId(), StatusProjeto.ABERTO, pageable);

        verify(projetoRepository).buscarVinculadosPorStatus(outroUsuario.getId(), StatusProjeto.ABERTO, pageable);
        verify(projetoRepository, never()).buscarVinculados(any(), any());
    }

    @Test
    void buscarPorId_deveRetornarDetalheQuandoEncontrado() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        stubDetalhe(projeto, 5, 2);

        ProjetoResponse response = projetoService.buscarPorId(projeto.getId());

        assertThat(response.id()).isEqualTo(projeto.getId());
        assertThat(response.totalMembros()).isEqualTo(5);
        assertThat(response.totalCandidaturasPendentes()).isEqualTo(2);
    }

    @Test
    void buscarPorId_deveLancarRecursoNaoEncontrado_quandoInativoOuInexistente() {
        UUID id = UUID.randomUUID();
        when(projetoRepository.findDetalhePorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projetoService.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ============================================
    // Uso interno
    // ============================================

    @Test
    void obterEntidadeAtiva_deveRetornarProjeto_quandoAtivo() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThat(projetoService.obterEntidadeAtiva(projeto.getId())).isEqualTo(projeto);
    }

    @Test
    void obterEntidadeAtiva_deveLancarRecursoNaoEncontrado_quandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(projetoRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projetoService.obterEntidadeAtiva(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void obterEntidadeComoDono_deveRetornarProjeto_quandoUsuarioForCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThat(projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId())).isEqualTo(projeto);
    }

    @Test
    void obterEntidadeComoDono_deveLancarRegraDeNegocio_quandoUsuarioNaoForCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.obterEntidadeComoDono(projeto.getId(), outroUsuario.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("permissão");
    }

    @Test
    void obterEntidadeComoDono_deveLancarRegraDeNegocio_quandoProjetoNaoTemCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        projeto.setCriador(null);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void ehCriador_deveRetornarTrue_quandoUsuarioForCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        assertThat(projetoService.ehCriador(projeto, criador.getId())).isTrue();
    }

    @Test
    void ehCriador_deveRetornarFalse_quandoUsuarioNaoForCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        assertThat(projetoService.ehCriador(projeto, outroUsuario.getId())).isFalse();
    }

    @Test
    void ehCriador_deveRetornarFalse_quandoProjetoNaoTemCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        projeto.setCriador(null);
        assertThat(projetoService.ehCriador(projeto, criador.getId())).isFalse();
    }

    // ============================================
    // atualizarBanner()
    // ============================================

    @Test
    void atualizarBanner_deveSalvarNovoBanner_eNaoTentarDeletarAntigo_quandoNaoHaviaBannerAnterior() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        MultipartFile arquivo = mock(MultipartFile.class);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(bannerStorageClient.salvar(arquivo)).thenReturn("http://storage/novo-banner.png");
        stubDetalhe(projeto, 0, 0);

        ProjetoResponse response = projetoService.atualizarBanner(projeto.getId(), criador.getId(), arquivo);

        assertThat(projeto.getBannerUrl()).isEqualTo("http://storage/novo-banner.png");
        assertThat(response.bannerUrl()).isEqualTo("http://storage/novo-banner.png");
        verify(bannerStorageClient).salvar(arquivo);
        verify(bannerStorageClient, never()).deletar(anyString());
    }

    @Test
    void atualizarBanner_deveDeletarBannerAntigo_quandoJaHaviaUm() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        projeto.setBannerUrl("http://storage/banner-antigo.png");
        MultipartFile arquivo = mock(MultipartFile.class);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(bannerStorageClient.salvar(arquivo)).thenReturn("http://storage/novo-banner.png");
        stubDetalhe(projeto, 0, 0);

        projetoService.atualizarBanner(projeto.getId(), criador.getId(), arquivo);

        verify(bannerStorageClient).deletar("http://storage/banner-antigo.png");
    }

    @Test
    void atualizarBanner_devePropagarExcecao_quandoUsuarioNaoForCriador() {
        Projeto projeto = EntityFixtures.projeto(criador, 5);
        MultipartFile arquivo = mock(MultipartFile.class);
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.atualizarBanner(projeto.getId(), outroUsuario.getId(), arquivo))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(bannerStorageClient, never()).salvar(any());
    }
}
