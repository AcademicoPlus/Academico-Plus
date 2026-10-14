package com.unifapi.academico.e2e;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercita a cadeia real de segurança ({@code GlobalRateLimitFilter} →
 * {@code JwtAuthenticationFilter} → {@code SecurityConfig}) contra Postgres real.
 * Exige Postgres rodando (ver {@link FluxoCompletoE2ETest} para detalhes de configuração).
 */
@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/academico_plus",
        "spring.datasource.username=academico_user",
        "spring.datasource.password=senha123",
        "spring.jpa.hibernate.ddl-auto=validate",
        "jwt.secret=e2e-test-secret-nao-usar-em-producao-0123456789abcdef",
        "jwt.expiration=3600000",
        "cors.allowed-origins=http://localhost:5173",
        "app.rate-limit.requisicoes-por-minuto=100000",
        "server.servlet.context-path=/api"
})
class SegurancaE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    /**
     * Sem nenhuma credencial apresentada: SecurityConfig não configura
     * httpBasic()/formLogin(), então o Spring Security cai no
     * AuthenticationEntryPoint padrão (Http403ForbiddenEntryPoint) e devolve 403.
     * Quando há um Bearer token, porém, quem responde é o JwtAuthenticationFilter
     * — e aí a resposta é 401 (ver testes abaixo).
     */
    @Test
    void deveRetornar403_quandoRequisicaoSemAuthorizationHeader() {
        ResponseEntity<String> resposta = restTemplate.getForEntity(url("/usuarios/me"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deveRetornar401_quandoTokenMalformado() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("token-completamente-invalido-e-mal-formado");

        ResponseEntity<String> resposta = restTemplate.exchange(
                url("/usuarios/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * O caso que mais acontece na prática: não há refresh token, então todo
     * usuário bate aqui quando o access token vence. Precisa ser 401 (e não o
     * 403 genérico de "sem permissão") para o frontend conseguir distinguir
     * sessão vencida de falta de permissão e mandar a pessoa para o login.
     */
    @Test
    void deveRetornar401_quandoTokenExpirado() {
        String tokenExpirado = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "ALUNO")
                .claim("type", "ACCESS")
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(
                        "e2e-test-secret-nao-usar-em-producao-0123456789abcdef".getBytes(StandardCharsets.UTF_8)))
                .compact();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenExpirado);

        ResponseEntity<String> resposta = restTemplate.exchange(
                url("/usuarios/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveRetornar200_quandoRotaPublicaSemAutenticacao() {
        ResponseEntity<String> resposta = restTemplate.getForEntity(url("/cursos"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
