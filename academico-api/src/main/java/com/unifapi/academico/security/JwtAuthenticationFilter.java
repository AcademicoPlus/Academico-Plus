package com.unifapi.academico.security;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.repository.TokenRevogadoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final TokenRevogadoRepository tokenRevogadoRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        
        if (tokenRevogadoRepository.existsByToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"erro\": \"Sessão encerrada. Faça login novamente.\"}");
            return;
        }

        if (jwtTokenProvider.validarToken(token, "ACCESS")) {
            UUID userId = jwtTokenProvider.obterSubjectDoToken(token);
            String roleDoToken = jwtTokenProvider.obterRoleDoToken(token);

            Usuario usuario = usuarioRepository.findByIdWithNivelAcesso(userId).orElse(null);

            // Suspensão aplicada no meio da sessão: o token continua válido e
            // assinado, então é aqui que ela precisa morder — do contrário o
            // suspenso seguiria usando a plataforma até o token expirar.
            if (usuario != null && usuario.estaSuspenso()) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"erro\": \"Sua conta está suspensa. Faça login para ver o motivo e o prazo.\"}");
                return;
            }

            if (usuario != null && Boolean.TRUE.equals(usuario.getAtivo())) {
                String roleAtual = usuario.getNivelAcesso().getNome();

                if (roleAtual.equals(roleDoToken)) {
                    var authority = new SimpleGrantedAuthority("ROLE_" + roleAtual.toUpperCase());
                    var authToken = new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            List.of(authority)
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}