package com.unifapi.academico.config;

import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita o uso de @PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GlobalRateLimitFilter globalRateLimitFilter;

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll() // Endpoints públicos de login, cadastro e reset
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // Health check do provedor de deploy (Render etc.) — não exige autenticação
                        .requestMatchers("/actuator/health").permitAll()
                        // Cursos precisam ficar visíveis antes do login: alimenta o dropdown da tela de cadastro
                        .requestMatchers(HttpMethod.GET, "/cursos/**").permitAll()
                        // Handshake SockJS/STOMP do chat: sem header Authorization (o JWT vai no
                        // CONNECT frame, validado pelo StompAuthChannelInterceptor, não aqui).
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Rate limit roda antes do JWT: até tentativa de flood em /auth/** (sem token) é barrada.
                .addFilterBefore(globalRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Compartilhado com WebSocketConfig, para o handshake STOMP/SockJS aceitar
     * as mesmas origens do CORS HTTP em vez de duplicar o parsing da env var.
     */
    @Bean
    public List<String> origensPermitidas() {
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList();
    }

    /**
     * Antes disso, cors.allowed-origins existia no .env mas não era lido em
     * nenhum lugar do código — o navegador bloqueava o frontend silenciosamente.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origensPermitidas());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Equivalente ao CryptContext(schemes=["argon2"]) do FastAPI
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}