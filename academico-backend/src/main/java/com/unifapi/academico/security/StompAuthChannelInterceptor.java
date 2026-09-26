package com.unifapi.academico.security;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.repository.ProjetoMembroRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.TokenRevogadoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autentica o CONNECT do STOMP com o mesmo JWT usado nas rotas REST — o
 * JwtAuthenticationFilter (OncePerRequestFilter) não roda para frames STOMP,
 * então essa validação (token revogado, assinatura, usuário ativo, role
 * atual) é repetida aqui a partir das mesmas dependências.
 *
 * Também autoriza o SUBSCRIBE em /topic/projetos/{id}: só o criador e os
 * membros atuais podem ouvir a sala. O envio (SEND) é autorizado no
 * ChatProjetoService, chamado pelo ChatProjetoController.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final TokenRevogadoRepository tokenRevogadoRepository;
    private final ProjetoRepository projetoRepository;
    private final ProjetoMembroRepository projetoMembroRepository;

    private static final Pattern TOPICO_PROJETO = Pattern.compile("^/topic/projetos/([0-9a-fA-F-]{36})$");

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(autenticar(accessor));
        }

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            autorizarInscricao(accessor.getDestination(), accessor.getUser());
        }

        return message;
    }

    private void autorizarInscricao(String destino, Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken token)
                || !(token.getPrincipal() instanceof Usuario usuario)) {
            throw new IllegalArgumentException("Conexão do chat não autenticada.");
        }

        Matcher matcher = destino != null ? TOPICO_PROJETO.matcher(destino) : null;
        if (matcher == null || !matcher.matches()) {
            throw new IllegalArgumentException("Destino de inscrição inválido.");
        }

        UUID projetoId = UUID.fromString(matcher.group(1));
        boolean temAcesso = projetoRepository.findByIdAndAtivoTrue(projetoId)
                .map(projeto -> (projeto.getCriador() != null && projeto.getCriador().getId().equals(usuario.getId()))
                        || projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projetoId, usuario.getId()))
                .orElse(false);

        if (!temAcesso) {
            throw new IllegalArgumentException("Você não tem acesso ao chat deste projeto.");
        }
    }

    private UsernamePasswordAuthenticationToken autenticar(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token de acesso ausente na conexão do chat.");
        }

        String token = authHeader.substring(7);

        if (tokenRevogadoRepository.existsByToken(token)) {
            throw new IllegalArgumentException("Sessão encerrada. Faça login novamente.");
        }

        if (!jwtTokenProvider.validarToken(token, "ACCESS")) {
            throw new IllegalArgumentException("Token de acesso inválido ou expirado.");
        }

        UUID userId = jwtTokenProvider.obterSubjectDoToken(token);
        String roleDoToken = jwtTokenProvider.obterRoleDoToken(token);

        Usuario usuario = usuarioRepository.findByIdWithNivelAcesso(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException("Usuário inativo.");
        }

        String roleAtual = usuario.getNivelAcesso().getNome();
        if (!roleAtual.equals(roleDoToken)) {
            throw new IllegalArgumentException("Permissões do usuário mudaram. Faça login novamente.");
        }

        var authority = new SimpleGrantedAuthority("ROLE_" + roleAtual.toUpperCase());
        return new UsernamePasswordAuthenticationToken(usuario, null, List.of(authority));
    }
}
