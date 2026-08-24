package com.unifapi.academico.e2e;

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
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
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
     * SecurityConfig não configura httpBasic()/formLogin(), então o Spring Security
     * cai no AuthenticationEntryPoint padrão (Http403ForbiddenEntryPoint) — requisição
     * sem autenticação vira 403, não 401, neste projeto.
     */
    @Test
    void deveRetornar403_quandoRequisicaoSemAuthorizationHeader() {
        ResponseEntity<String> resposta = restTemplate.getForEntity(url("/usuarios/me"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deveRetornar403_quandoTokenMalformado() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("token-completamente-invalido-e-mal-formado");

        ResponseEntity<String> resposta = restTemplate.exchange(
                url("/usuarios/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deveRetornar200_quandoRotaPublicaSemAutenticacao() {
        ResponseEntity<String> resposta = restTemplate.getForEntity(url("/cursos"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
