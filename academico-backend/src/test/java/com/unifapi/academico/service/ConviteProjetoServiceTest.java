package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.enums.StatusConvite;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Candidatura;
import com.unifapi.academico.domain.models.ConviteProjeto;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.convite.ConviteResponse;
import com.unifapi.academico.dto.convite.CriarConviteRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CandidaturaRepository;
import com.unifapi.academico.repository.ConviteProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.support.EntityFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConviteProjetoServiceTest {

    @Mock
    private ConviteProjetoRepository conviteRepository;

    @Mock
    private CandidaturaRepository candidaturaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProjetoService projetoService;

    @Mock
    private ProjetoMembroService projetoMembroService;

    @InjectMocks
    private ConviteProjetoService conviteService;

    private Usuario criador;
    private Usuario convidado;
    private Projeto projeto;
    private UUID projetoId;
    private UUID criadorId;
    private UUID convidadoId;

    @BeforeEach
    void setUp() {
        NivelAcesso nivelAcesso = EntityFixtures.nivelAcesso("Aluno");
        criador = EntityFixtures.usuario("criador@academico.com", nivelAcesso);
        convidado = EntityFixtures.usuario("convidado@academico.com", nivelAcesso);
        projeto = EntityFixtures.projeto(criador, 3);

        projetoId = projeto.getId();
        criadorId = criador.getId();
        convidadoId = convidado.getId();
    }

    private ConviteProjeto convitePendente() {
        return ConviteProjeto.builder()
                .id(UUID.randomUUID())
                .projeto(projeto)
                .convidado(convidado)
                .convidante(criador)
                .funcao("Backend")
                .status(StatusConvite.PENDENTE)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    private void prepararConvidarValido() {
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);
        when(usuarioRepository.findById(convidadoId)).thenReturn(Optional.of(convidado));
        when(projetoService.ehCriador(projeto, convidadoId)).thenReturn(false);
    }

    // ============================================
    // convidar()
    // ============================================

    @Test
    void convidar_deveCriarConvitePendente_quandoDadosValidos() {
        prepararConvidarValido();
        when(conviteRepository.save(any(ConviteProjeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ConviteResponse resposta = conviteService.convidar(
                new CriarConviteRequest(projetoId, convidadoId, "  Backend  ", "   "), criadorId);

        ArgumentCaptor<ConviteProjeto> captor = ArgumentCaptor.forClass(ConviteProjeto.class);
        verify(conviteRepository).save(captor.capture());
        assertThat(captor.getValue().getConvidante()).isEqualTo(criador);
        assertThat(captor.getValue().getConvidado()).isEqualTo(convidado);
        assertThat(captor.getValue().getFuncao()).isEqualTo("Backend");
        assertThat(captor.getValue().getMensagem()).isNull();
        assertThat(resposta.status()).isEqualTo(StatusConvite.PENDENTE);
    }

    @Test
    void convidar_deveFalhar_quandoConvidadoEhOCriador() {
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);
        when(usuarioRepository.findById(criadorId)).thenReturn(Optional.of(criador));
        when(projetoService.ehCriador(projeto, criadorId)).thenReturn(true);

        assertThatThrownBy(() -> conviteService.convidar(
                new CriarConviteRequest(projetoId, criadorId, null, null), criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("si mesmo");
    }

    @Test
    void convidar_deveFalhar_quandoUsuarioNaoExiste() {
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);
        when(usuarioRepository.findById(convidadoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conviteService.convidar(
                new CriarConviteRequest(projetoId, convidadoId, null, null), criadorId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void convidar_deveFalhar_quandoProjetoConcluido() {
        projeto.setStatus(StatusProjeto.CONCLUIDO);
        prepararConvidarValido();

        assertThatThrownBy(() -> conviteService.convidar(
                new CriarConviteRequest(projetoId, convidadoId, null, null), criadorId))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(conviteRepository, never()).save(any());
    }

    @Test
    void convidar_devePermitir_quandoProjetoEmAndamento() {
        projeto.setStatus(StatusProjeto.EM_ANDAMENTO);
        prepararConvidarValido();
        when(conviteRepository.save(any(ConviteProjeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ConviteResponse resposta = conviteService.convidar(
                new CriarConviteRequest(projetoId, convidadoId, null, null), criadorId);

        assertThat(resposta.status()).isEqualTo(StatusConvite.PENDENTE);
    }

    @Test
    void convidar_deveFalhar_quandoVagasEsgotadas() {
        projeto.setVagasPreenchidas(3);
        prepararConvidarValido();

        assertThatThrownBy(() -> conviteService.convidar(
                new CriarConviteRequest(projetoId, convidadoId, null, null), criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("vagas");
    }

    @Test
    void convidar_deveFalhar_quandoJaEhMembro() {
        prepararConvidarValido();
        when(projetoMembroService.ehMembro(projetoId, convidadoId)).thenReturn(true);

        assertThatThrownBy(() -> conviteService.convidar(
                new CriarConviteRequest(projetoId, convidadoId, null, null), criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já é membro");
    }

    @Test
    void convidar_deveFalhar_quandoJaExisteConvitePendente() {
        prepararConvidarValido();
        when(conviteRepository.existsByProjeto_IdAndConvidado_IdAndStatus(
                projetoId, convidadoId, StatusConvite.PENDENTE)).thenReturn(true);

        assertThatThrownBy(() -> conviteService.convidar(
                new CriarConviteRequest(projetoId, convidadoId, null, null), criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("convite pendente");
    }

    @Test
    void convidar_deveFalhar_quandoUsuarioJaSeCandidatou() {
        prepararConvidarValido();
        when(candidaturaRepository.existsByProjeto_IdAndUsuario_IdAndStatus(
                projetoId, convidadoId, StatusCandidatura.PENDENTE)).thenReturn(true);

        assertThatThrownBy(() -> conviteService.convidar(
                new CriarConviteRequest(projetoId, convidadoId, null, null), criadorId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Aceite a candidatura");
    }

    // ============================================
    // aceitar()
    // ============================================

    @Test
    void aceitar_deveAdicionarMembroEMarcarAceito() {
        ConviteProjeto convite = convitePendente();
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));
        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(candidaturaRepository.findByProjeto_IdAndUsuario_IdAndStatus(
                projetoId, convidadoId, StatusCandidatura.PENDENTE)).thenReturn(Optional.empty());
        when(conviteRepository.save(any(ConviteProjeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ConviteResponse resposta = conviteService.aceitar(convite.getId(), convidadoId);

        verify(projetoMembroService).adicionarMembro(projeto, convidado, "Backend");
        assertThat(resposta.status()).isEqualTo(StatusConvite.ACEITO);
        assertThat(resposta.respondidoEm()).isNotNull();
    }

    @Test
    void aceitar_deveAceitarCandidaturaPendenteDoConvidado() {
        ConviteProjeto convite = convitePendente();
        Candidatura candidatura = EntityFixtures.candidatura(projeto, convidado, StatusCandidatura.PENDENTE);
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));
        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(candidaturaRepository.findByProjeto_IdAndUsuario_IdAndStatus(
                projetoId, convidadoId, StatusCandidatura.PENDENTE)).thenReturn(Optional.of(candidatura));
        when(conviteRepository.save(any(ConviteProjeto.class))).thenAnswer(inv -> inv.getArgument(0));

        conviteService.aceitar(convite.getId(), convidadoId);

        assertThat(candidatura.getStatus()).isEqualTo(StatusCandidatura.ACEITO);
        verify(candidaturaRepository).save(candidatura);
    }

    @Test
    void aceitar_deveFalhar_quandoUsuarioNaoEhOConvidado() {
        ConviteProjeto convite = convitePendente();
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> conviteService.aceitar(convite.getId(), criadorId))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(projetoMembroService, never()).adicionarMembro(any(), any(), any());
    }

    @Test
    void aceitar_deveFalhar_quandoConviteJaRespondido() {
        ConviteProjeto convite = convitePendente();
        convite.setStatus(StatusConvite.CANCELADO);
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> conviteService.aceitar(convite.getId(), convidadoId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("cancelado");
        verify(projetoMembroService, never()).adicionarMembro(any(), any(), any());
    }

    @Test
    void aceitar_devePropagarErro_quandoVagasEsgotaramDepoisDoConvite() {
        ConviteProjeto convite = convitePendente();
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));
        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(projetoMembroService.adicionarMembro(projeto, convidado, "Backend"))
                .thenThrow(new RegraDeNegocioException("O projeto já preencheu todas as 3 vaga(s) disponíveis."));

        assertThatThrownBy(() -> conviteService.aceitar(convite.getId(), convidadoId))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(convite.getStatus()).isEqualTo(StatusConvite.PENDENTE);
        verify(conviteRepository, never()).save(any());
    }

    @Test
    void aceitar_deveFalhar_quandoConviteNaoExiste() {
        UUID id = UUID.randomUUID();
        when(conviteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conviteService.aceitar(id, convidadoId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ============================================
    // recusar() / cancelar()
    // ============================================

    @Test
    void recusar_deveMarcarRecusado() {
        ConviteProjeto convite = convitePendente();
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));
        when(conviteRepository.save(any(ConviteProjeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ConviteResponse resposta = conviteService.recusar(convite.getId(), convidadoId);

        assertThat(resposta.status()).isEqualTo(StatusConvite.RECUSADO);
        verify(projetoMembroService, never()).adicionarMembro(any(), any(), any());
    }

    @Test
    void recusar_deveFalhar_quandoUsuarioNaoEhOConvidado() {
        ConviteProjeto convite = convitePendente();
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> conviteService.recusar(convite.getId(), UUID.randomUUID()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void cancelar_deveMarcarCancelado_quandoCriador() {
        ConviteProjeto convite = convitePendente();
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));
        when(projetoService.obterEntidadeComoDono(projetoId, criadorId)).thenReturn(projeto);
        when(conviteRepository.save(any(ConviteProjeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ConviteResponse resposta = conviteService.cancelar(convite.getId(), criadorId);

        assertThat(resposta.status()).isEqualTo(StatusConvite.CANCELADO);
    }

    @Test
    void cancelar_deveFalhar_quandoNaoEhDono() {
        ConviteProjeto convite = convitePendente();
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));
        when(projetoService.obterEntidadeComoDono(eq(projetoId), eq(convidadoId)))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        assertThatThrownBy(() -> conviteService.cancelar(convite.getId(), convidadoId))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(conviteRepository, never()).save(any());
    }

    // ============================================
    // buscarPorId()
    // ============================================

    @Test
    void buscarPorId_deveFalhar_quandoTerceiro() {
        ConviteProjeto convite = convitePendente();
        UUID terceiro = UUID.randomUUID();
        when(conviteRepository.findById(convite.getId())).thenReturn(Optional.of(convite));
        when(projetoService.ehCriador(projeto, terceiro)).thenReturn(false);

        assertThatThrownBy(() -> conviteService.buscarPorId(convite.getId(), terceiro))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
