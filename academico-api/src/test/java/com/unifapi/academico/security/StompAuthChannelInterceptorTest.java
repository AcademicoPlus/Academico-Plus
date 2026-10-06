package com.unifapi.academico.security;

import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.repository.ProjetoMembroRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.TokenRevogadoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.support.EntityFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Autorização do SUBSCRIBE: só criador e membros atuais ouvem /topic/projetos/{id}. */
@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenRevogadoRepository tokenRevogadoRepository;
    @Mock private ProjetoRepository projetoRepository;
    @Mock private ProjetoMembroRepository projetoMembroRepository;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    private final MessageChannel canal = mock(MessageChannel.class);
    private Usuario criador;
    private Usuario outro;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        NivelAcesso nivel = EntityFixtures.nivelAcesso("ALUNO");
        criador = EntityFixtures.usuario("criador@academico.com", nivel);
        outro = EntityFixtures.usuario("outro@academico.com", nivel);
        projeto = EntityFixtures.projeto(criador, 3);
    }

    private Message<byte[]> subscribe(String destino, Usuario usuario) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destino);
        accessor.setSubscriptionId("sub-0");
        if (usuario != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void subscribe_devePermitir_quandoCriador() {
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));

        assertThatCode(() -> interceptor.preSend(subscribe("/topic/projetos/" + projeto.getId(), criador), canal))
                .doesNotThrowAnyException();
    }

    @Test
    void subscribe_devePermitir_quandoMembro() {
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), outro.getId())).thenReturn(true);

        assertThatCode(() -> interceptor.preSend(subscribe("/topic/projetos/" + projeto.getId(), outro), canal))
                .doesNotThrowAnyException();
    }

    @Test
    void subscribe_deveNegar_quandoNaoParticipa() {
        when(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).thenReturn(Optional.of(projeto));
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), outro.getId())).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/projetos/" + projeto.getId(), outro), canal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não tem acesso");
    }

    @Test
    void subscribe_deveNegar_quandoProjetoInativo() {
        UUID id = UUID.randomUUID();
        when(projetoRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/projetos/" + id, criador), canal))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subscribe_deveNegar_quandoDestinoForaDoPadrao() {
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/outra-coisa", criador), canal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Destino");
    }

    @Test
    void subscribe_deveNegar_quandoSemUsuarioNaSessao() {
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/projetos/" + projeto.getId(), null), canal))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
