package com.unifapi.academico.e2e;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Curso;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.dto.auth.CadastroRequest;
import com.unifapi.academico.dto.auth.CadastroResponse;
import com.unifapi.academico.dto.auth.LoginRequest;
import com.unifapi.academico.dto.auth.LoginResponse;
import com.unifapi.academico.dto.avaliacao.AvaliacaoResponse;
import com.unifapi.academico.dto.avaliacao.AvaliarRequest;
import com.unifapi.academico.dto.candidatura.CandidaturaResponse;
import com.unifapi.academico.dto.candidatura.CriarCandidaturaRequest;
import com.unifapi.academico.dto.projeto.AlterarStatusProjetoRequest;
import com.unifapi.academico.dto.projeto.CriarProjetoRequest;
import com.unifapi.academico.dto.projeto.ProjetoResponse;
import com.unifapi.academico.repository.CursoRepository;
import com.unifapi.academico.repository.NivelAcessoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxo ponta-a-ponta completo, contra Postgres real (não H2) — exercita a stack
 * inteira: {@code GlobalRateLimitFilter}, {@code JwtAuthenticationFilter},
 * {@code SecurityConfig} e todas as camadas de serviço.
 *
 * Exige Postgres rodando (local: {@code docker-compose up -d} no diretório do backend;
 * CI: serviço Postgres do workflow, ver .github/workflows/backend-ci.yml).
 *
 * {@code spring.flyway.enabled=false} porque o único migration file
 * (V1_schema_inicial.sql) não segue a convenção de nome do Flyway e é ignorado (ver
 * "Limitações conhecidas" no README do backend) — {@code ddl-auto=create-drop} cria o
 * schema do zero a cada execução, e por isso os dados de seed (níveis de acesso,
 * cursos) que normalmente viriam da migração precisam ser inseridos manualmente
 * no {@link #seed()} abaixo.
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
class FluxoCompletoE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NivelAcessoRepository nivelAcessoRepository;

    @Autowired
    private CursoRepository cursoRepository;

    private UUID idCurso;

    @BeforeEach
    void seed() {
        if (nivelAcessoRepository.findByNome("ALUNO").isEmpty()) {
            nivelAcessoRepository.save(NivelAcesso.builder().nome("ALUNO").descricao("Aluno").build());
        }
        Curso curso = cursoRepository.findByNomeIgnoreCase("Ciência da Computação")
                .orElseGet(() -> cursoRepository.save(
                        Curso.builder().nome("Ciência da Computação").sigla("CC").ativo(true).build()));
        idCurso = curso.getId();
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private String cadastrarELogar(String email) {
        CadastroRequest cadastro = new CadastroRequest(
                email, "Usuario E2E", 3, idCurso, "senhaSegura123", true);
        ResponseEntity<CadastroResponse> respostaCadastro = restTemplate.postForEntity(
                url("/auth/cadastro"), cadastro, CadastroResponse.class);
        assertThat(respostaCadastro.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respostaCadastro.getBody()).isNotNull();
        assertThat(respostaCadastro.getBody().sucesso()).isTrue();

        LoginRequest login = new LoginRequest(email, "senhaSegura123");
        ResponseEntity<LoginResponse> respostaLogin = restTemplate.postForEntity(
                url("/auth/login"), login, LoginResponse.class);
        assertThat(respostaLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respostaLogin.getBody()).isNotNull();
        assertThat(respostaLogin.getBody().sucesso()).isTrue();
        return respostaLogin.getBody().token();
    }

    private HttpHeaders comToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void deveExecutarFluxoCompleto_cadastroLoginProjetoCandidaturaAvaliacao() {
        String emailCriador = "criador-" + UUID.randomUUID() + "@unifapi.edu.br";
        String emailCandidato = "candidato-" + UUID.randomUUID() + "@unifapi.edu.br";

        String tokenCriador = cadastrarELogar(emailCriador);
        String tokenCandidato = cadastrarELogar(emailCandidato);

        // 1. Criador cria um projeto
        CriarProjetoRequest criarProjeto = new CriarProjetoRequest(
                "Projeto de robótica educacional", "Descrição com pelo menos vinte caracteres para o teste.",
                2, null, null);
        ResponseEntity<ProjetoResponse> respostaProjeto = restTemplate.exchange(
                url("/projetos"), HttpMethod.POST,
                new HttpEntity<>(criarProjeto, comToken(tokenCriador)), ProjetoResponse.class);
        assertThat(respostaProjeto.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID projetoId = respostaProjeto.getBody().id();

        // 2. Candidato se candidata
        CriarCandidaturaRequest candidatar = new CriarCandidaturaRequest(projetoId, "Tenho muito interesse!");
        ResponseEntity<CandidaturaResponse> respostaCandidatura = restTemplate.exchange(
                url("/candidaturas"), HttpMethod.POST,
                new HttpEntity<>(candidatar, comToken(tokenCandidato)), CandidaturaResponse.class);
        assertThat(respostaCandidatura.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID candidaturaId = respostaCandidatura.getBody().id();

        // 3. Criador aceita a candidatura
        ResponseEntity<CandidaturaResponse> respostaAceite = restTemplate.exchange(
                url("/candidaturas/" + candidaturaId + "/aceitar"), HttpMethod.PATCH,
                new HttpEntity<>(null, comToken(tokenCriador)), CandidaturaResponse.class);
        assertThat(respostaAceite.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respostaAceite.getBody().status().name()).isEqualTo("ACEITO");

        // 4. Criador conclui o projeto
        AlterarStatusProjetoRequest concluir = new AlterarStatusProjetoRequest(StatusProjeto.CONCLUIDO, null);
        ResponseEntity<ProjetoResponse> respostaConclusao = restTemplate.exchange(
                url("/projetos/" + projetoId + "/status"), HttpMethod.PATCH,
                new HttpEntity<>(concluir, comToken(tokenCriador)), ProjetoResponse.class);
        assertThat(respostaConclusao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respostaConclusao.getBody().status()).isEqualTo(StatusProjeto.CONCLUIDO);

        // 5. Criador avalia o candidato (agora membro)
        ResponseEntity<String> perfilCandidato = restTemplate.exchange(
                url("/usuarios/me"), HttpMethod.GET, new HttpEntity<>(comToken(tokenCandidato)), String.class);
        assertThat(perfilCandidato.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<com.unifapi.academico.dto.usuario.UsuarioPerfilResponse> perfilCandidatoTipado =
                restTemplate.exchange(url("/usuarios/me"), HttpMethod.GET,
                        new HttpEntity<>(comToken(tokenCandidato)),
                        com.unifapi.academico.dto.usuario.UsuarioPerfilResponse.class);
        UUID idCandidato = perfilCandidatoTipado.getBody().id();

        AvaliarRequest avaliar = new AvaliarRequest(idCandidato, 5, "Ótimo colaborador!");
        ResponseEntity<AvaliacaoResponse> respostaAvaliacao = restTemplate.exchange(
                url("/projetos/" + projetoId + "/avaliacoes"), HttpMethod.POST,
                new HttpEntity<>(avaliar, comToken(tokenCriador)), AvaliacaoResponse.class);
        assertThat(respostaAvaliacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respostaAvaliacao.getBody().nota()).isEqualTo(5);
    }
}
