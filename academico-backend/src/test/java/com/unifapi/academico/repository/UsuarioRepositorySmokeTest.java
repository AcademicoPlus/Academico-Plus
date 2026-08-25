package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.Curso;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste descartável: prova que as Specifications dinâmicas de {@link UsuarioRepository}
 * (usadas por {@code UsuarioService.buscarPerfisPublicos}) rodam corretamente contra H2,
 * seguindo o mesmo padrão de {@code QueriesSmokeTest}.
 *
 * Nota: {@code Usuario} tem {@code @SQLRestriction("ativo = true")} a nível de entidade —
 * toda consulta (inclusive via Specification) já filtra usuários inativos automaticamente.
 * Por isso não há teste para {@code comStatus("inativos")}/{@code "excluidos"}: qualquer
 * usuário com {@code ativo=false} é invisível a qualquer query, tornando esses filtros
 * inalcançáveis na prática — uma particularidade do modelo, não um bug deste teste.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:usuariosmoke;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class UsuarioRepositorySmokeTest {

    private static final Pageable PAGE = PageRequest.of(0, 10);

    @Autowired private TestEntityManager em;
    @Autowired private UsuarioRepository usuarioRepository;

    private NivelAcesso aluno;
    private NivelAcesso admin;
    private Curso sistemasDeInformacao;
    private Curso cienciaDaComputacao;
    private Usuario carlos;
    private Usuario ana;
    private Usuario adminUser;

    @BeforeEach
    void fixture() {
        aluno = em.persist(NivelAcesso.builder().nome("ALUNO").build());
        admin = em.persist(NivelAcesso.builder().nome("ADMIN").build());

        sistemasDeInformacao = em.persist(Curso.builder().nome("Sistemas de Informacao").sigla("SI").build());
        cienciaDaComputacao = em.persist(Curso.builder().nome("Ciencia da Computacao").sigla("CC").build());

        carlos = em.persist(novoUsuario("carlos@unifapi.edu.br", "Carlos Eduardo", aluno, sistemasDeInformacao));
        ana = em.persist(novoUsuario("ana@unifapi.edu.br", "Ana Paula", aluno, cienciaDaComputacao));
        adminUser = em.persist(novoUsuario("admin@unifapi.edu.br", "Administrador Geral", admin, null));

        em.flush();
        em.clear();
    }

    @Test
    void somenteAtivos_deveIncluirTodosOsUsuariosAtivosSemDeletadoEm() {
        List<Usuario> resultado = usuarioRepository.findAll(UsuarioRepository.somenteAtivos());

        assertThat(resultado).extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("carlos@unifapi.edu.br", "ana@unifapi.edu.br", "admin@unifapi.edu.br");
    }

    @Test
    void semAdmin_deveExcluirUsuariosComNivelAdmin() {
        List<Usuario> resultado = usuarioRepository.findAll(UsuarioRepository.semAdmin());

        assertThat(resultado).extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("carlos@unifapi.edu.br", "ana@unifapi.edu.br");
    }

    @Test
    void comCurso_deveFiltrarPorCursoQuandoInformado() {
        List<Usuario> resultado = usuarioRepository.findAll(UsuarioRepository.comCurso(sistemasDeInformacao.getId()));

        assertThat(resultado).extracting(Usuario::getEmail).containsExactly("carlos@unifapi.edu.br");
    }

    @Test
    void comCurso_deveRetornarTodosQuandoIdForNulo() {
        List<Usuario> resultado = usuarioRepository.findAll(UsuarioRepository.comCurso(null));

        assertThat(resultado).hasSize(3);
    }

    @Test
    void comBuscaTextual_deveEncontrarPorNomeOuEmailCaseInsensitive() {
        assertThat(usuarioRepository.findAll(UsuarioRepository.comBuscaTextual("CARLOS")))
                .extracting(Usuario::getEmail).containsExactly("carlos@unifapi.edu.br");

        assertThat(usuarioRepository.findAll(UsuarioRepository.comBuscaTextual("ana@unifapi")))
                .extracting(Usuario::getEmail).containsExactly("ana@unifapi.edu.br");

        assertThat(usuarioRepository.findAll(UsuarioRepository.comBuscaTextual("inexistente")))
                .isEmpty();
    }

    @Test
    void comBuscaTextual_deveRetornarTodosQuandoNuloOuEmBranco() {
        assertThat(usuarioRepository.findAll(UsuarioRepository.comBuscaTextual(null))).hasSize(3);
        assertThat(usuarioRepository.findAll(UsuarioRepository.comBuscaTextual("  "))).hasSize(3);
    }

    @Test
    void especificacoesCombinadas_devemFiltrarAlunosDoCursoComBuscaTextual() {
        Specification<Usuario> combinada = Specification
                .where(UsuarioRepository.somenteAtivos())
                .and(UsuarioRepository.semAdmin())
                .and(UsuarioRepository.comCurso(sistemasDeInformacao.getId()))
                .and(UsuarioRepository.comBuscaTextual("carlos"));

        var pagina = usuarioRepository.findAll(combinada, PAGE);

        assertThat(pagina.getContent()).extracting(Usuario::getEmail).containsExactly("carlos@unifapi.edu.br");
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findTodosComHabilidades_deveRetornarUsuariosComColecaoDeHabilidadesInicializada() {
        List<Usuario> resultado = usuarioRepository.findTodosComHabilidades();

        assertThat(resultado).hasSize(3);
        assertThat(resultado).allSatisfy(u -> assertThat(u.getHabilidades()).isNotNull());
    }

    @Test
    void existsByEmail_deveDiferenciarEmailCadastradoDeInexistente() {
        assertThat(usuarioRepository.existsByEmail("carlos@unifapi.edu.br")).isTrue();
        assertThat(usuarioRepository.existsByEmail("inexistente@unifapi.edu.br")).isFalse();
    }

    @Test
    void findByIdWithNivelAcesso_deveRetornarUsuarioComNivelAcessoCarregado() {
        var encontrado = usuarioRepository.findByIdWithNivelAcesso(carlos.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNivelAcesso().getNome()).isEqualTo("ALUNO");
    }

    @Test
    void findByIdWithNivelAcesso_deveRetornarVazioQuandoIdNaoExiste() {
        assertThat(usuarioRepository.findByIdWithNivelAcesso(java.util.UUID.randomUUID())).isEmpty();
    }

    private static Usuario novoUsuario(String email, String nome, NivelAcesso nivel, Curso curso) {
        return Usuario.builder()
                .email(email).nome(nome).senhaHash("hash")
                .nivelAcesso(nivel).curso(curso).ativo(true)
                .build();
    }
}
