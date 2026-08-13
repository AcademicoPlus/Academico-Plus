package com.unifapi.academico.support;

import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * Substitui os dois filtros de segurança reais (JWT, rate limit) por no-ops nos slice tests
 * de controller ({@code @WebMvcTest}). {@code SecurityConfig} é {@code @EnableWebSecurity}, e
 * {@code @WebMvcTest} também escaneia beans do tipo {@code Filter} — sem isso, o contexto do
 * slice tentaria construir os filtros reais, que dependem de repositórios JPA fora do slice.
 *
 * As regras de {@code authorizeHttpRequests}/{@code @PreAuthorize} continuam ativas
 * normalmente (vêm da própria {@code SecurityConfig}, que É carregada); só os dois filtros
 * customizados viram passagem direta, porque {@link WithUsuarioMock} já popula o
 * SecurityContext diretamente, sem precisar do {@code JwtAuthenticationFilter} real.
 *
 * Uso em cada teste de controller:
 * <pre>
 * {@code
 * @WebMvcTest(
 *     controllers = XController.class,
 *     excludeFilters = @ComponentScan.Filter(
 *         type = FilterType.ASSIGNABLE_TYPE,
 *         classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
 *     )
 * )
 * @Import(SecurityTestConfig.class)
 * class XControllerTest { ... }
 * }
 * </pre>
 */
@TestConfiguration
public class SecurityTestConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(null, null, null) {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                chain.doFilter(request, response);
            }
        };
    }

    @Bean
    public GlobalRateLimitFilter globalRateLimitFilter() {
        return new GlobalRateLimitFilter(Integer.MAX_VALUE) {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                chain.doFilter(request, response);
            }
        };
    }
}
