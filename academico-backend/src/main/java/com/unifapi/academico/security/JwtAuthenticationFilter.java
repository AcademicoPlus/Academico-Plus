package com.unifapi.academico.security;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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

        if (jwtTokenProvider.validarToken(token, "ACCESS")) {
            UUID userId = jwtTokenProvider.obterSubjectDoToken(token);
            String roleDoToken = jwtTokenProvider.obterRoleDoToken(token);

            // Busca no banco e valida estado (assim como no get_current_user do FastAPI)
            Usuario usuario = usuarioRepository.findByIdWithNivelAcesso(userId).orElse(null);

            if (usuario != null && Boolean.TRUE.equals(usuario.getAtivo())) {
                String roleAtual = usuario.getNivelAcesso().getNome();

                // Garante que o token é invalidado caso o perfil de acesso tenha mudado no banco
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