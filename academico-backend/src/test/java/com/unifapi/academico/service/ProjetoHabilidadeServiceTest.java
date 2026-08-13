package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.ProjetoHabilidade;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.habilidade.ProjetoHabilidadeResponse;
import com.unifapi.academico.dto.habilidade.VincularHabilidadeRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.ProjetoHabilidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.unifapi.academico.support.EntityFixtures.habilidade;
import static com.unifapi.academico.support.EntityFixtures.nivelAcesso;
import static com.unifapi.academico.support.EntityFixtures.projeto;
import static com.unifapi.academico.support.EntityFixtures.projetoHabilidade;
import static com.unifapi.academico.support.EntityFixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (Mockito puro, sem contexto Spring) para {@link ProjetoHabilidadeService}.
 */
@ExtendWith(MockitoExtension.class)
class ProjetoHabilidadeServiceTest {

    @Mock
    private ProjetoHabilidadeRepository projetoHabilidadeRepository;

    @Mock
    private ProjetoService projetoService;

    @Mock
    private HabilidadeService habilidadeService;

    private ProjetoHabilidadeService service;

    private NivelAcesso nivelAcesso;
    private Usuario criador;
    private Projeto projeto;
    private Habilidade habilidade;

    @BeforeEach
    void setUp() {
        service = new ProjetoHabilidadeService(projetoHabilidadeRepository, projetoService, habilidadeService);

        nivelAcesso = nivelAcesso("ALUNO");
        criador = usuario("criador@teste.com", nivelAcesso);
        projeto = projeto(criador, 3);
        habilidade = habilidade("Java", "Backend");
    }

    // ============================================
    // listarPorProjeto()
    // ============================================

    @Test
    void deveListarHabilidadesDoProjetoQuandoProjetoEstaAtivo() {
        when(projetoService.obterEntidadeAtiva(projeto.getId())).thenReturn(projeto);
        ProjetoHabilidade vinculo = projetoHabilidade(projeto, habilidade, true);
        when(projetoHabilidadeRepository.findByProjeto_IdOrderByHabilidade_NomeAsc(projeto.getId()))
                .thenReturn(List.of(vinculo));

        List<ProjetoHabilidadeResponse> resultado = service.listarPorProjeto(projeto.getId());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).obrigatoria()).isTrue();
    }

    @Test
    void deveLancarExcecaoAoListarHabilidadesDeProjetoInativoOuInexistente() {
        UUID projetoId = UUID.randomUUID();
        when(projetoService.obterEntidadeAtiva(projetoId))
                .thenThrow(new RecursoNaoEncontradoException("Projeto não encontrado com o ID informado."));

        assertThatThrownBy(() -> service.listarPorProjeto(projetoId))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(projetoHabilidadeRepository, never()).findByProjeto_IdOrderByHabilidade_NomeAsc(any());
    }

    // ============================================
    // vincular()
    // ============================================

    @Test
    void deveLancarExcecaoAoVincularHabilidadeInexistente() {
        UUID habilidadeInexistenteId = UUID.randomUUID();
        VincularHabilidadeRequest request = new VincularHabilidadeRequest(habilidadeInexistenteId, true);
        when(projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId())).thenReturn(projeto);
        when(habilidadeService.obterEntidade(habilidadeInexistenteId))
                .thenThrow(new RecursoNaoEncontradoException("Habilidade não encontrada com o ID informado."));

        assertThatThrownBy(() -> service.vincular(projeto.getId(), request, criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(projetoHabilidadeRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoVincularHabilidadeJaVinculadaAoProjeto() {
        VincularHabilidadeRequest request = new VincularHabilidadeRequest(habilidade.getId(), true);
        when(projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId())).thenReturn(projeto);
        when(habilidadeService.obterEntidade(habilidade.getId())).thenReturn(habilidade);
        when(projetoHabilidadeRepository.existsByProjeto_IdAndHabilidade_Id(projeto.getId(), habilidade.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.vincular(projeto.getId(), request, criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já está vinculada a este projeto");

        verify(projetoHabilidadeRepository, never()).save(any());
        verify(habilidadeService, never()).recalcularContadores(any());
    }

    @Test
    void deveLancarExcecaoAoVincularQuandoUsuarioLogadoNaoEDono() {
        UUID estranhoId = UUID.randomUUID();
        VincularHabilidadeRequest request = new VincularHabilidadeRequest(habilidade.getId(), true);
        when(projetoService.obterEntidadeComoDono(projeto.getId(), estranhoId))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        assertThatThrownBy(() -> service.vincular(projeto.getId(), request, estranhoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não tem permissão");

        verify(habilidadeService, never()).obterEntidade(any());
        verify(projetoHabilidadeRepository, never()).save(any());
    }

    @Test
    void deveDefinirObrigatoriaComoFalsoPorPadraoQuandoNaoInformada() {
        VincularHabilidadeRequest request = new VincularHabilidadeRequest(habilidade.getId(), null);
        when(projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId())).thenReturn(projeto);
        when(habilidadeService.obterEntidade(habilidade.getId())).thenReturn(habilidade);
        when(projetoHabilidadeRepository.existsByProjeto_IdAndHabilidade_Id(projeto.getId(), habilidade.getId()))
                .thenReturn(false);
        when(projetoHabilidadeRepository.save(any(ProjetoHabilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjetoHabilidadeResponse response = service.vincular(projeto.getId(), request, criador.getId());

        assertThat(response.obrigatoria()).isFalse();
    }

    @Test
    void deveVincularHabilidadeComSucessoEDispararRecalculoDeContadores() {
        VincularHabilidadeRequest request = new VincularHabilidadeRequest(habilidade.getId(), true);
        when(projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId())).thenReturn(projeto);
        when(habilidadeService.obterEntidade(habilidade.getId())).thenReturn(habilidade);
        when(projetoHabilidadeRepository.existsByProjeto_IdAndHabilidade_Id(projeto.getId(), habilidade.getId()))
                .thenReturn(false);
        when(projetoHabilidadeRepository.save(any(ProjetoHabilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjetoHabilidadeResponse response = service.vincular(projeto.getId(), request, criador.getId());

        assertThat(response.obrigatoria()).isTrue();
        assertThat(response.habilidade().id()).isEqualTo(habilidade.getId());
        verify(projetoHabilidadeRepository).flush();
        verify(habilidadeService).recalcularContadores(habilidade.getId());
    }

    /**
     * O service delega a validação de "projeto existe/está ativo" para
     * ProjetoService.obterEntidadeComoDono, que verifica apenas a flag `ativo`
     * (findByIdAndAtivoTrue) — não o campo `status`. Um projeto CONCLUIDO ou
     * CANCELADO que ainda esteja ativo=true (i.e., não foi "inativado" via
     * ProjetoService.inativar) continua aceitando novos vínculos de requisitos,
     * pois não há nenhuma verificação de status em vincular().
     */
    @ParameterizedTest(name = "vincular() não bloqueia projeto com status {0} (apenas ativo=true é exigido)")
    @EnumSource(value = StatusProjeto.class, names = {"CONCLUIDO", "CANCELADO"})
    void devePermitirVincularHabilidadeAProjetoConcluidoOuCanceladoDesdeQueAtivo(StatusProjeto status) {
        Projeto projetoFinalizado = projeto(criador, "Projeto finalizado", 3, status);
        VincularHabilidadeRequest request = new VincularHabilidadeRequest(habilidade.getId(), false);
        when(projetoService.obterEntidadeComoDono(projetoFinalizado.getId(), criador.getId()))
                .thenReturn(projetoFinalizado);
        when(habilidadeService.obterEntidade(habilidade.getId())).thenReturn(habilidade);
        when(projetoHabilidadeRepository.existsByProjeto_IdAndHabilidade_Id(projetoFinalizado.getId(), habilidade.getId()))
                .thenReturn(false);
        when(projetoHabilidadeRepository.save(any(ProjetoHabilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjetoHabilidadeResponse response = service.vincular(projetoFinalizado.getId(), request, criador.getId());

        assertThat(response).isNotNull();
        verify(projetoHabilidadeRepository).save(any(ProjetoHabilidade.class));
    }

    // ============================================
    // alterarObrigatoriedade()
    // ============================================

    @Test
    void deveLancarExcecaoAoAlterarObrigatoriedadeDeVinculoInexistente() {
        when(projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId())).thenReturn(projeto);
        when(projetoHabilidadeRepository.findByProjeto_IdAndHabilidade_Id(projeto.getId(), habilidade.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alterarObrigatoriedade(projeto.getId(), habilidade.getId(), true, criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("não está vinculada ao projeto informado");

        verify(projetoHabilidadeRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoAlterarObrigatoriedadeQuandoUsuarioLogadoNaoEDono() {
        UUID estranhoId = UUID.randomUUID();
        when(projetoService.obterEntidadeComoDono(projeto.getId(), estranhoId))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        assertThatThrownBy(() -> service.alterarObrigatoriedade(projeto.getId(), habilidade.getId(), true, estranhoId))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(projetoHabilidadeRepository, never()).findByProjeto_IdAndHabilidade_Id(any(), any());
    }

    @Test
    void deveAlterarObrigatoriedadeDoVinculoExistente() {
        ProjetoHabilidade vinculo = projetoHabilidade(projeto, habilidade, false);
        when(projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId())).thenReturn(projeto);
        when(projetoHabilidadeRepository.findByProjeto_IdAndHabilidade_Id(projeto.getId(), habilidade.getId()))
                .thenReturn(Optional.of(vinculo));
        when(projetoHabilidadeRepository.save(any(ProjetoHabilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjetoHabilidadeResponse response =
                service.alterarObrigatoriedade(projeto.getId(), habilidade.getId(), true, criador.getId());

        assertThat(response.obrigatoria()).isTrue();
        ArgumentCaptor<ProjetoHabilidade> captor = ArgumentCaptor.forClass(ProjetoHabilidade.class);
        verify(projetoHabilidadeRepository).save(captor.capture());
        assertThat(captor.getValue().getObrigatoria()).isTrue();
    }

    // ============================================
    // desvincular()
    // ============================================

    @Test
    void deveLancarExcecaoAoDesvincularVinculoInexistente() {
        when(projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId())).thenReturn(projeto);
        when(projetoHabilidadeRepository.findByProjeto_IdAndHabilidade_Id(projeto.getId(), habilidade.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desvincular(projeto.getId(), habilidade.getId(), criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("não está vinculada ao projeto informado");

        verify(projetoHabilidadeRepository, never()).delete(any());
        verify(habilidadeService, never()).recalcularContadores(any());
    }

    @Test
    void deveLancarExcecaoAoDesvincularQuandoUsuarioLogadoNaoEDono() {
        UUID estranhoId = UUID.randomUUID();
        when(projetoService.obterEntidadeComoDono(projeto.getId(), estranhoId))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        assertThatThrownBy(() -> service.desvincular(projeto.getId(), habilidade.getId(), estranhoId))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(projetoHabilidadeRepository, never()).findByProjeto_IdAndHabilidade_Id(any(), any());
        verify(projetoHabilidadeRepository, never()).delete(any());
    }

    @Test
    void deveDesvincularHabilidadeExistenteEDispararRecalculoDeContadores() {
        ProjetoHabilidade vinculo = projetoHabilidade(projeto, habilidade, true);
        when(projetoService.obterEntidadeComoDono(projeto.getId(), criador.getId())).thenReturn(projeto);
        when(projetoHabilidadeRepository.findByProjeto_IdAndHabilidade_Id(projeto.getId(), habilidade.getId()))
                .thenReturn(Optional.of(vinculo));

        service.desvincular(projeto.getId(), habilidade.getId(), criador.getId());

        verify(projetoHabilidadeRepository).delete(vinculo);
        verify(projetoHabilidadeRepository).flush();
        verify(habilidadeService).recalcularContadores(habilidade.getId());
    }
}
