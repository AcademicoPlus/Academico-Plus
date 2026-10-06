package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.LeituraChatProjeto;
import com.unifapi.academico.domain.models.MensagemProjeto;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.mensagem.ConversaResponse;
import com.unifapi.academico.dto.mensagem.NaoLidasPorProjeto;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.LeituraChatProjetoRepository;
import com.unifapi.academico.repository.MensagemProjetoRepository;
import com.unifapi.academico.repository.ProjetoRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatProjetoServiceTest {

    @Mock private MensagemProjetoRepository mensagemProjetoRepository;
    @Mock private LeituraChatProjetoRepository leituraChatProjetoRepository;
    @Mock private ProjetoRepository projetoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProjetoService projetoService;
    @Mock private ProjetoMembroService projetoMembroService;

    @InjectMocks
    private ChatProjetoService chatProjetoService;

    private Usuario eu;
    private Usuario colega;
    private Projeto projetoAntigo;
    private Projeto projetoNovo;
    private Projeto projetoSemMensagens;

    @BeforeEach
    void setUp() {
        NivelAcesso nivel = EntityFixtures.nivelAcesso("ALUNO");
        eu = EntityFixtures.usuario("eu@academico.com", nivel);
        colega = EntityFixtures.usuario("colega@academico.com", nivel);
        projetoAntigo = EntityFixtures.projeto(colega, 3);
        projetoNovo = EntityFixtures.projeto(eu, 3);
        projetoSemMensagens = EntityFixtures.projeto(eu, 3);
    }

    private MensagemProjeto mensagem(Projeto projeto, Usuario autor, LocalDateTime quando) {
        return MensagemProjeto.builder()
                .id(UUID.randomUUID()).projeto(projeto).autor(autor).conteudo("oi").criadoEm(quando).build();
    }

    @Test
    void listarConversas_deveOrdenarPorAtividadeEAnexarNaoLidas() {
        LocalDateTime agora = LocalDateTime.now();
        when(projetoRepository.listarVinculados(eu.getId()))
                .thenReturn(List.of(projetoSemMensagens, projetoAntigo, projetoNovo));
        when(mensagemProjetoRepository.buscarUltimasMensagens(anyCollection())).thenReturn(List.of(
                mensagem(projetoAntigo, colega, agora.minusDays(1)),
                mensagem(projetoNovo, colega, agora)));
        when(mensagemProjetoRepository.contarNaoLidas(eq(eu.getId()), anyCollection()))
                .thenReturn(List.of(new NaoLidasPorProjeto(projetoNovo.getId(), 2L)));

        List<ConversaResponse> conversas = chatProjetoService.listarConversas(eu.getId());

        assertThat(conversas).extracting(c -> c.projeto().id())
                .containsExactly(projetoNovo.getId(), projetoAntigo.getId(), projetoSemMensagens.getId());
        assertThat(conversas.get(0).naoLidas()).isEqualTo(2);
        assertThat(conversas.get(1).naoLidas()).isZero();
        assertThat(conversas.get(2).ultimaMensagem()).isNull();
    }

    @Test
    void listarConversas_semProjetos_naoConsultaMensagens() {
        when(projetoRepository.listarVinculados(eu.getId())).thenReturn(List.of());

        assertThat(chatProjetoService.listarConversas(eu.getId())).isEmpty();
        verifyNoInteractions(mensagemProjetoRepository);
    }

    @Test
    void contarNaoLidas_deveSomarTodasAsConversas() {
        when(projetoRepository.listarVinculados(eu.getId())).thenReturn(List.of(projetoAntigo, projetoNovo));
        when(mensagemProjetoRepository.contarNaoLidas(eq(eu.getId()), anyCollection())).thenReturn(List.of(
                new NaoLidasPorProjeto(projetoAntigo.getId(), 3L),
                new NaoLidasPorProjeto(projetoNovo.getId(), 1L)));

        assertThat(chatProjetoService.contarNaoLidas(eu.getId())).isEqualTo(4);
    }

    @Test
    void marcarComoLida_deveCriarRegistro_quandoPrimeiraLeitura() {
        when(projetoService.obterEntidadeAtiva(projetoNovo.getId())).thenReturn(projetoNovo);
        when(projetoService.ehCriador(projetoNovo, eu.getId())).thenReturn(true);
        when(leituraChatProjetoRepository.findByProjeto_IdAndUsuario_Id(projetoNovo.getId(), eu.getId()))
                .thenReturn(Optional.empty());
        when(usuarioRepository.getReferenceById(eu.getId())).thenReturn(eu);

        chatProjetoService.marcarComoLida(projetoNovo.getId(), eu.getId());

        ArgumentCaptor<LeituraChatProjeto> captor = ArgumentCaptor.forClass(LeituraChatProjeto.class);
        verify(leituraChatProjetoRepository).save(captor.capture());
        assertThat(captor.getValue().getProjeto()).isEqualTo(projetoNovo);
        assertThat(captor.getValue().getUsuario()).isEqualTo(eu);
        assertThat(captor.getValue().getLidoAte()).isNotNull();
    }

    @Test
    void marcarComoLida_deveAvancarRegistroExistente() {
        LocalDateTime antes = LocalDateTime.now().minusHours(1);
        LeituraChatProjeto existente = LeituraChatProjeto.builder()
                .id(UUID.randomUUID()).projeto(projetoAntigo).usuario(eu).lidoAte(antes).build();
        when(projetoService.obterEntidadeAtiva(projetoAntigo.getId())).thenReturn(projetoAntigo);
        when(projetoService.ehCriador(projetoAntigo, eu.getId())).thenReturn(false);
        when(projetoMembroService.ehMembro(projetoAntigo.getId(), eu.getId())).thenReturn(true);
        when(leituraChatProjetoRepository.findByProjeto_IdAndUsuario_Id(projetoAntigo.getId(), eu.getId()))
                .thenReturn(Optional.of(existente));

        chatProjetoService.marcarComoLida(projetoAntigo.getId(), eu.getId());

        assertThat(existente.getLidoAte()).isAfter(antes);
        verify(leituraChatProjetoRepository).save(existente);
    }

    @Test
    void marcarComoLida_deveFalhar_quandoSemAcesso() {
        UUID estranho = UUID.randomUUID();
        when(projetoService.obterEntidadeAtiva(projetoAntigo.getId())).thenReturn(projetoAntigo);
        when(projetoService.ehCriador(projetoAntigo, estranho)).thenReturn(false);
        when(projetoMembroService.ehMembro(projetoAntigo.getId(), estranho)).thenReturn(false);

        assertThatThrownBy(() -> chatProjetoService.marcarComoLida(projetoAntigo.getId(), estranho))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(leituraChatProjetoRepository, never()).save(any());
    }
}
