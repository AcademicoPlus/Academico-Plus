package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Candidatura;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.candidatura.AceitarCandidaturaRequest;
import com.unifapi.academico.dto.candidatura.CandidaturaResponse;
import com.unifapi.academico.dto.candidatura.CriarCandidaturaRequest;
import com.unifapi.academico.dto.candidatura.RejeitarCandidaturaRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CandidaturaRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.support.EntityFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidaturaServiceTest {

    @Mock
    private CandidaturaRepository candidaturaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProjetoService projetoService;

    @Mock
    private ProjetoMembroService projetoMembroService;

    @InjectMocks
    private CandidaturaService candidaturaService;

    private Usuario criador;
    private Usuario candidato;
    private Projeto projeto;
    private UUID projetoId;
    private UUID criadorId;
    private UUID candidatoId;

    @BeforeEach
    void setUp() {
        NivelAcesso nivelAcesso = EntityFixtures.nivelAcesso("Aluno");
        criador = EntityFixtures.usuario("criador@academico.com", nivelAcesso);
        candidato = EntityFixtures.usuario("candidato@academico.com", nivelAcesso);
        projeto = EntityFixtures.projeto(criador, 3);

        projetoId = projeto.getId();
        criadorId = criador.getId();
        candidatoId = candidato.getId();
    }

    // ============================================
    // candidatar()
    // ============================================

    @Test
    void deveLancarExcecaoAoCandidatarSeProjetoNaoEncontradoOuInativo() {
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(projetoId, "Quero participar");
        when(projetoService.obterEntidadeAtiva(projetoId))
                .thenThrow(new RecursoNaoEncontradoException("Projeto não encontrado com o ID informado."));

        assertThatThrownBy(() -> candidaturaService.candidatar(request, candidatoId))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Projeto não encontrado");

        verify(usuarioRepository, never()).findById(any());
        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoCandidatarSeUsuarioLogadoNaoEncontrado() {
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(projetoId, null);
        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(usuarioRepository.findById(candidatoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidaturaService.candidatar(request, candidatoId))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoCriadorTentaCandidatarAoProprioProjeto() {
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(projetoId, null);
        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(usuarioRepository.findById(criadorId)).thenReturn(Optional.of(criador));
        when(projetoService.ehCriador(projeto, criadorId)).thenReturn(true);

        assertThatThrownBy(() -> candidaturaService.candidatar(request, criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("criado por você");

        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoProjetoNaoEstaAberto() {
        Projeto projetoEmAndamento = EntityFixtures.projeto(
                criador, "Projeto em andamento", 3, StatusProjeto.EM_ANDAMENTO);
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(projetoEmAndamento.getId(), null);

        when(projetoService.obterEntidadeAtiva(projetoEmAndamento.getId())).thenReturn(projetoEmAndamento);
        when(usuarioRepository.findById(candidatoId)).thenReturn(Optional.of(candidato));
        when(projetoService.ehCriador(projetoEmAndamento, candidatoId)).thenReturn(false);

        assertThatThrownBy(() -> candidaturaService.candidatar(request, candidatoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não está aberto para candidaturas");

        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoProjetoJaPreencheuTodasAsVagas() {
        projeto.setVagas(1);
        projeto.setVagasPreenchidas(1);
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(projetoId, null);

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(usuarioRepository.findById(candidatoId)).thenReturn(Optional.of(candidato));
        when(projetoService.ehCriador(projeto, candidatoId)).thenReturn(false);

        assertThatThrownBy(() -> candidaturaService.candidatar(request, candidatoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("preencheu todas as vagas");

        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioJaEhMembroDoProjeto() {
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(projetoId, null);

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(usuarioRepository.findById(candidatoId)).thenReturn(Optional.of(candidato));
        when(projetoService.ehCriador(projeto, candidatoId)).thenReturn(false);
        when(projetoMembroService.ehMembro(projetoId, candidatoId)).thenReturn(true);

        assertThatThrownBy(() -> candidaturaService.candidatar(request, candidatoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já é membro deste projeto");

        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoTentarCandidatarNovamenteAposRejeicao() {
        // Simula que já existe uma candidatura anterior (ex.: REJEITADA) para o
        // par (projeto, usuario). A regra só consulta existência, então uma
        // rejeição anterior bloqueia qualquer nova tentativa de candidatura.
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(projetoId, null);

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(usuarioRepository.findById(candidatoId)).thenReturn(Optional.of(candidato));
        when(projetoService.ehCriador(projeto, candidatoId)).thenReturn(false);
        when(projetoMembroService.ehMembro(projetoId, candidatoId)).thenReturn(false);
        when(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projetoId, candidatoId)).thenReturn(true);

        assertThatThrownBy(() -> candidaturaService.candidatar(request, candidatoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já possui uma candidatura registrada");

        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveCriarCandidaturaComSucessoENormalizarMensagem() {
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(projetoId, "  Quero participar  ");

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(usuarioRepository.findById(candidatoId)).thenReturn(Optional.of(candidato));
        when(projetoService.ehCriador(projeto, candidatoId)).thenReturn(false);
        when(projetoMembroService.ehMembro(projetoId, candidatoId)).thenReturn(false);
        when(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projetoId, candidatoId)).thenReturn(false);
        when(candidaturaRepository.save(any(Candidatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidaturaResponse response = candidaturaService.candidatar(request, candidatoId);

        ArgumentCaptor<Candidatura> captor = ArgumentCaptor.forClass(Candidatura.class);
        verify(candidaturaRepository).save(captor.capture());
        Candidatura salva = captor.getValue();

        assertThat(salva.getStatus()).isEqualTo(StatusCandidatura.PENDENTE);
        assertThat(salva.getMensagem()).isEqualTo("Quero participar");
        assertThat(salva.getProjeto()).isEqualTo(projeto);
        assertThat(salva.getUsuario()).isEqualTo(candidato);
        assertThat(salva.getDataCandidatura()).isNotNull();
        assertThat(response.status()).isEqualTo(StatusCandidatura.PENDENTE);
    }

    @Test
    void deveNormalizarMensagemEmBrancoParaNuloAoCandidatar() {
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(projetoId, "   ");

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(usuarioRepository.findById(candidatoId)).thenReturn(Optional.of(candidato));
        when(projetoService.ehCriador(projeto, candidatoId)).thenReturn(false);
        when(projetoMembroService.ehMembro(projetoId, candidatoId)).thenReturn(false);
        when(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projetoId, candidatoId)).thenReturn(false);
        when(candidaturaRepository.save(any(Candidatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        candidaturaService.candidatar(request, candidatoId);

        ArgumentCaptor<Candidatura> captor = ArgumentCaptor.forClass(Candidatura.class);
        verify(candidaturaRepository).save(captor.capture());
        assertThat(captor.getValue().getMensagem()).isNull();
    }

    // ============================================
    // cancelar()
    // ============================================

    @Test
    void deveLancarExcecaoAoCancelarCandidaturaInexistente() {
        UUID candidaturaId = UUID.randomUUID();
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidaturaService.cancelar(candidaturaId, candidatoId))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(candidaturaRepository, never()).delete(any());
    }

    @Test
    void deveLancarExcecaoAoCancelarCandidaturaDeOutroUsuario() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));

        // O criador do projeto tenta cancelar a candidatura de outra pessoa.
        assertThatThrownBy(() -> candidaturaService.cancelar(candidatura.getId(), criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("só pode cancelar as suas próprias candidaturas");

        verify(candidaturaRepository, never()).delete(any());
    }

    @Test
    void deveLancarExcecaoAoCancelarCandidaturaJaAceita() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.ACEITO);
        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));

        assertThatThrownBy(() -> candidaturaService.cancelar(candidatura.getId(), candidatoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não pode ser cancelada");

        verify(candidaturaRepository, never()).delete(any());
    }

    @Test
    void deveLancarExcecaoAoCancelarCandidaturaJaRejeitada() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.REJEITADO);
        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));

        assertThatThrownBy(() -> candidaturaService.cancelar(candidatura.getId(), candidatoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não pode ser cancelada");

        verify(candidaturaRepository, never()).delete(any());
    }

    @Test
    void deveCancelarCandidaturaPendenteComSucessoViaExclusaoDefinitiva() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));

        candidaturaService.cancelar(candidatura.getId(), candidatoId);

        verify(candidaturaRepository, times(1)).delete(candidatura);
        verify(candidaturaRepository, never()).save(any());
    }

    // ============================================
    // aceitar()
    // ============================================

    @Test
    void deveLancarExcecaoAoAceitarCandidaturaInexistente() {
        UUID candidaturaId = UUID.randomUUID();
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidaturaService.aceitar(candidaturaId, null, criadorId))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(projetoMembroService);
    }

    @Test
    void deveLancarExcecaoAoAceitarSeQuemChamaNaoEhCriadorDoProjeto() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.obterEntidadeComoDono(projetoId, candidatoId))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        assertThatThrownBy(() -> candidaturaService.aceitar(candidatura.getId(), null, candidatoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não tem permissão");

        verifyNoInteractions(projetoMembroService);
        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoAceitarCandidaturaJaRespondida() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.ACEITO);
        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);

        assertThatThrownBy(() -> candidaturaService.aceitar(candidatura.getId(), null, criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já foi respondida");

        verifyNoInteractions(projetoMembroService);
        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void naoDeveAlterarStatusDaCandidaturaQuandoAdicionarMembroFalhar() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        AceitarCandidaturaRequest request = new AceitarCandidaturaRequest("Dev");

        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);
        when(projetoMembroService.adicionarMembro(projeto, candidato, "Dev"))
                .thenThrow(new RegraDeNegocioException("O projeto já preencheu todas as 3 vaga(s) disponíveis."));

        assertThatThrownBy(() -> candidaturaService.aceitar(candidatura.getId(), request, criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("preencheu todas as");

        // Nenhum efeito colateral deve escapar da falha: a candidatura continua pendente
        // e nunca é salva com o novo status.
        assertThat(candidatura.getStatus()).isEqualTo(StatusCandidatura.PENDENTE);
        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveAceitarCandidaturaComSucesso() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        candidatura.setMotivoRejeicao("motivo antigo irrelevante");
        AceitarCandidaturaRequest request = new AceitarCandidaturaRequest("Desenvolvedor");

        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);
        when(candidaturaRepository.save(any(Candidatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidaturaResponse response = candidaturaService.aceitar(candidatura.getId(), request, criadorId);

        verify(projetoMembroService).adicionarMembro(projeto, candidato, "Desenvolvedor");

        ArgumentCaptor<Candidatura> captor = ArgumentCaptor.forClass(Candidatura.class);
        verify(candidaturaRepository).save(captor.capture());
        Candidatura salva = captor.getValue();

        assertThat(salva.getStatus()).isEqualTo(StatusCandidatura.ACEITO);
        assertThat(salva.getMotivoRejeicao()).isNull();
        assertThat(salva.getDataResposta()).isNotNull();
        assertThat(response.status()).isEqualTo(StatusCandidatura.ACEITO);
    }

    @Test
    void deveAceitarCandidaturaComRequestNuloSemLancarNpeEPassarFuncaoNula() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);

        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);
        when(candidaturaRepository.save(any(Candidatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        candidaturaService.aceitar(candidatura.getId(), null, criadorId);

        verify(projetoMembroService).adicionarMembro(eq(projeto), eq(candidato), eq(null));
    }

    // ============================================
    // rejeitar()
    // ============================================

    @Test
    void deveLancarExcecaoAoRejeitarCandidaturaInexistente() {
        UUID candidaturaId = UUID.randomUUID();
        RejeitarCandidaturaRequest request = new RejeitarCandidaturaRequest("Perfil não compatível");
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidaturaService.rejeitar(candidaturaId, request, criadorId))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(projetoMembroService);
    }

    @Test
    void deveLancarExcecaoAoRejeitarSeQuemChamaNaoEhCriadorDoProjeto() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        RejeitarCandidaturaRequest request = new RejeitarCandidaturaRequest("Perfil não compatível");

        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.obterEntidadeComoDono(projetoId, candidatoId))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        assertThatThrownBy(() -> candidaturaService.rejeitar(candidatura.getId(), request, candidatoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não tem permissão");

        verifyNoInteractions(projetoMembroService);
        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoRejeitarCandidaturaJaRespondida() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.REJEITADO);
        RejeitarCandidaturaRequest request = new RejeitarCandidaturaRequest("Perfil não compatível");

        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);

        assertThatThrownBy(() -> candidaturaService.rejeitar(candidatura.getId(), request, criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já foi respondida");

        verifyNoInteractions(projetoMembroService);
        verify(candidaturaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarCandidaturaComSucessoESemAlterarMembrosOuVagas() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        RejeitarCandidaturaRequest request = new RejeitarCandidaturaRequest("  Perfil não compatível com a vaga  ");

        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);
        when(candidaturaRepository.save(any(Candidatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidaturaResponse response = candidaturaService.rejeitar(candidatura.getId(), request, criadorId);

        ArgumentCaptor<Candidatura> captor = ArgumentCaptor.forClass(Candidatura.class);
        verify(candidaturaRepository).save(captor.capture());
        Candidatura salva = captor.getValue();

        assertThat(salva.getStatus()).isEqualTo(StatusCandidatura.REJEITADO);
        assertThat(salva.getMotivoRejeicao()).isEqualTo("Perfil não compatível com a vaga");
        assertThat(salva.getDataResposta()).isNotNull();
        assertThat(response.status()).isEqualTo(StatusCandidatura.REJEITADO);

        // Rejeitar não deve criar vínculo de membro nem tocar em vagas.
        verifyNoInteractions(projetoMembroService);
    }

    // ============================================
    // buscarPorId()
    // ============================================

    @Test
    void deveLancarExcecaoAoBuscarCandidaturaInexistente() {
        UUID candidaturaId = UUID.randomUUID();
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidaturaService.buscarPorId(candidaturaId, candidatoId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveRetornarCandidaturaQuandoSolicitanteEhOProprioCandidato() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.ehCriador(projeto, candidatoId)).thenReturn(false);

        CandidaturaResponse response = candidaturaService.buscarPorId(candidatura.getId(), candidatoId);

        assertThat(response.id()).isEqualTo(candidatura.getId());
    }

    @Test
    void deveRetornarCandidaturaQuandoSolicitanteEhOCriadorDoProjeto() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.ehCriador(projeto, criadorId)).thenReturn(true);

        CandidaturaResponse response = candidaturaService.buscarPorId(candidatura.getId(), criadorId);

        assertThat(response.id()).isEqualTo(candidatura.getId());
    }

    @Test
    void deveLancarExcecaoAoBuscarCandidaturaSemSerCandidatoNemCriador() {
        Candidatura candidatura = EntityFixtures.candidatura(projeto, candidato, StatusCandidatura.PENDENTE);
        UUID outroUsuarioId = UUID.randomUUID();

        when(candidaturaRepository.findById(candidatura.getId())).thenReturn(Optional.of(candidatura));
        when(projetoService.ehCriador(projeto, outroUsuarioId)).thenReturn(false);

        assertThatThrownBy(() -> candidaturaService.buscarPorId(candidatura.getId(), outroUsuarioId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não tem permissão para visualizar");
    }
}
