package com.unifapi.academico.support;

import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Usuario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Injeta um {@link Usuario} autenticado no contexto de segurança de um
 * {@code @WebMvcTest}, do mesmo jeito que o {@code JwtAuthenticationFilter} faz em
 * produção: o principal é a própria entidade {@code Usuario} (não um username), com
 * uma única authority {@code "ROLE_" + nivelAcesso}. Necessário porque os
 * controllers usam {@code @AuthenticationPrincipal Usuario usuarioLogado} direto.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithUsuarioMock.Factory.class)
public @interface WithUsuarioMock {

    String role() default "ALUNO";

    String email() default "usuario.teste@unifapi.edu.br";

    String nome() default "Usuario Teste";

    class Factory implements WithSecurityContextFactory<WithUsuarioMock> {

        @Override
        public SecurityContext createSecurityContext(WithUsuarioMock annotation) {
            NivelAcesso nivelAcesso = NivelAcesso.builder()
                    .id(UUID.randomUUID())
                    .nome(annotation.role())
                    .build();

            Usuario usuario = Usuario.builder()
                    .id(UUID.randomUUID())
                    .email(annotation.email())
                    .nome(annotation.nome())
                    .senhaHash("$2a$10$hashDeTeste")
                    .nivelAcesso(nivelAcesso)
                    .ativo(true)
                    .totalAvaliacoes(0)
                    .termosAceitosEm(LocalDateTime.now())
                    .criadoEm(LocalDateTime.now())
                    .build();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    usuario,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role().toUpperCase()))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            return context;
        }
    }
}
