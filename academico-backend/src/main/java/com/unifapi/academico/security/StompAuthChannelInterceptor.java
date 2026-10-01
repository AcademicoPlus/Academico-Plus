package com.unifapi.academico.security;

import com.unifapi.academico.domain.models.Usuario;
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

import java.util.List;
import java.util.UUID;

/**
 * Autentica o CONNECT do STOMP com o mesmo JWT usado nas rotas REST — o
 * JwtAuthenticationFilter (OncePerRequestFilter) não roda para frames STOMP,
 * então essa validação (token revogado, assinatura, usuário ativo, role
 * atual) é repetida aqui a partir das mesmas dependências. Autorização por
 * projeto continua responsabilidade do controller (ChatProjetoController),
 * não deste interceptor.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final TokenRevogadoRepository tokenRevogadoRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(autenticar(accessor));
        }

        return message;
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
