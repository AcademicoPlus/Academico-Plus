package com.unifapi.academico.repository;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste descartável: executa todas as queries customizadas dos repositories
 * contra H2, para provar que elas rodam (e não só que compilam/parseiam).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:smoke;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class QueriesSmokeTest {

    private static final Pageable PAGE = PageRequest.of(0, 10);

    @Autowired private TestEntityManager em;
    @Autowired private ProjetoRepository projetoRepository;
    @Autowired private CandidaturaRepository candidaturaRepository;
    @Autowired private ProjetoMembroRepository projetoMembroRepository;
    @Autowired private ProjetoHabilidadeRepository projetoHabilidadeRepository;
    @Autowired private HabilidadeRepository habilidadeRepository;
    @Autowired private UsuarioHabilidadeRepository usuarioHabilidadeRepository;

    private Usuario criador;
    private Usuario candidato;
    private Usuario membro;
    private Projeto projeto;
    private Habilidade java;

    @BeforeEach
    void fixture() {
        NivelAcesso aluno = em.persist(NivelAcesso.builder().nome("ALUNO").build());
        Curso curso = em.persist(Curso.builder().nome("Sistemas de Informacao").sigla("SI").build());

        criador = em.persist(novoUsuario("criador@unifapi.edu.br", "Criador", aluno, curso));
        candidato = em.persist(novoUsuario("candidato@unifapi.edu.br", "Candidato", aluno, curso));
        membro = em.persist(novoUsuario("membro@unifapi.edu.br", "Membro", aluno, curso));

        java = em.persist(Habilidade.builder().nome("Java").categoria("Backend").build());
        Habilidade angular = em.persist(Habilidade.builder().nome("Angular").categoria("Frontend").build());

        projeto = em.persist(Projeto.builder()
                .criador(criador)
                .titulo("Projeto de robotica educacional")
                .descricao("Descricao suficientemente longa para o projeto de robotica.")
                .status(StatusProjeto.ABERTO)
                .vagas(3)
                .vagasPreenchidas(1)
                .ativo(true)
                .build());

        em.persist(ProjetoHabilidade.builder().projeto(projeto).habilidade(java).obrigatoria(true).build());
        em.persist(ProjetoHabilidade.builder().projeto(projeto).habilidade(angular).obrigatoria(false).build());

        em.persist(ProjetoMembro.builder()
                .projeto(projeto).usuario(membro).funcao("Dev").dataAdesao(LocalDateTime.now()).build());

        em.persist(Candidatura.builder()
                .projeto(projeto).usuario(candidato)
                .status(StatusCandidatura.PENDENTE)
                .mensagem("Tenho interesse")
                .dataCandidatura(LocalDateTime.now())
                .build());

        em.flush();
        em.clear();
    }

    @Test
    void projetoRepository_executaTodasAsQueries() {
        // O ponto crítico: filtro de enum opcional (:status IS NULL OR ...)
        assertThat(projetoRepository.buscarAtivos(null, null, PAGE)).hasSize(1);
        assertThat(projetoRepository.buscarAtivos(StatusProjeto.ABERTO, null, PAGE)).hasSize(1);
        assertThat(projetoRepository.buscarAtivos(StatusProjeto.CONCLUIDO, null, PAGE)).isEmpty();

        // Busca textual opcional, case-insensitive, em titulo e descricao
        assertThat(projetoRepository.buscarAtivos(null, "ROBOTICA", PAGE)).hasSize(1);
        assertThat(projetoRepository.buscarAtivos(null, "suficientemente", PAGE)).hasSize(1);
        assertThat(projetoRepository.buscarAtivos(null, "inexistente", PAGE)).isEmpty();
        assertThat(projetoRepository.buscarAtivos(StatusProjeto.ABERTO, "robotica", PAGE)).hasSize(1);

        assertThat(projetoRepository.findByCriador_IdAndAtivoTrue(criador.getId(), PAGE)).hasSize(1);
        assertThat(projetoRepository.findByCriador_IdAndAtivoTrue(candidato.getId(), PAGE)).isEmpty();

        assertThat(projetoRepository.findByMembros_Usuario_IdAndAtivoTrue(membro.getId(), PAGE)).hasSize(1);
        assertThat(projetoRepository.findByMembros_Usuario_IdAndAtivoTrue(candidato.getId(), PAGE)).isEmpty();

        assertThat(projetoRepository.countByCriador_IdAndAtivoTrue(criador.getId())).isEqualTo(1);
        assertThat(projetoRepository.findByIdAndAtivoTrue(projeto.getId())).isPresent();

        // EntityGraph do detalhe deve trazer criador + habilidades inicializados
        var detalhe = projetoRepository.findDetalhePorId(projeto.getId()).orElseThrow();
        assertThat(detalhe.getCriador().getNome()).isEqualTo("Criador");
        assertThat(detalhe.getHabilidadesNecessarias()).hasSize(2);
        assertThat(detalhe.podeAceitarCandidato()).isTrue();
    }

    @Test
    void candidaturaRepository_executaTodasAsQueries() {
        assertThat(candidaturaRepository.findByProjeto_Id(projeto.getId(), PAGE)).hasSize(1);
        assertThat(candidaturaRepository.findByProjeto_IdAndStatus(
                projeto.getId(), StatusCandidatura.PENDENTE, PAGE)).hasSize(1);
        assertThat(candidaturaRepository.findByProjeto_IdAndStatus(
                projeto.getId(), StatusCandidatura.ACEITO, PAGE)).isEmpty();

        assertThat(candidaturaRepository.findByUsuario_Id(candidato.getId(), PAGE)).hasSize(1);
        assertThat(candidaturaRepository.findByUsuario_IdAndStatus(
                candidato.getId(), StatusCandidatura.PENDENTE, PAGE)).hasSize(1);

        assertThat(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(
                projeto.getId(), candidato.getId())).isTrue();
        assertThat(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(
                projeto.getId(), membro.getId())).isFalse();

        assertThat(candidaturaRepository.countByProjeto_IdAndStatus(
                projeto.getId(), StatusCandidatura.PENDENTE)).isEqualTo(1);
        assertThat(candidaturaRepository.countByUsuario_IdAndStatus(
                candidato.getId(), StatusCandidatura.PENDENTE)).isEqualTo(1);

        var umaCandidatura = candidaturaRepository.findByProjeto_Id(projeto.getId(), PAGE)
                .getContent().get(0);
        assertThat(candidaturaRepository.findById(umaCandidatura.getId())).isPresent();
    }

    @Test
    void membroEHabilidadeRepositories_executamTodasAsQueries() {
        assertThat(projetoMembroRepository.findByProjeto_IdOrderByDataAdesaoAsc(projeto.getId())).hasSize(1);
        assertThat(projetoMembroRepository.findByProjeto_IdAndUsuario_Id(
                projeto.getId(), membro.getId())).isPresent();
        assertThat(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(
                projeto.getId(), membro.getId())).isTrue();
        assertThat(projetoMembroRepository.countByProjeto_Id(projeto.getId())).isEqualTo(1);
        assertThat(projetoMembroRepository.countByUsuario_Id(membro.getId())).isEqualTo(1);

        // Ordenação aninhada por nome da habilidade (Angular antes de Java)
        var vinculos = projetoHabilidadeRepository.findByProjeto_IdOrderByHabilidade_NomeAsc(projeto.getId());
        assertThat(vinculos).hasSize(2);
        assertThat(vinculos.get(0).getHabilidade().getNome()).isEqualTo("Angular");
        assertThat(vinculos.get(1).getHabilidade().getNome()).isEqualTo("Java");

        assertThat(projetoHabilidadeRepository.findByProjeto_IdAndHabilidade_Id(
                projeto.getId(), java.getId())).isPresent();
        assertThat(projetoHabilidadeRepository.existsByProjeto_IdAndHabilidade_Id(
                projeto.getId(), java.getId())).isTrue();
        assertThat(projetoHabilidadeRepository.countByHabilidade_Id(java.getId())).isEqualTo(1);
        assertThat(projetoHabilidadeRepository.countByProjeto_Id(projeto.getId())).isEqualTo(2);

        // Catálogo: filtros opcionais de busca e categoria
        assertThat(habilidadeRepository.buscar(null, null, PAGE)).hasSize(2);
        assertThat(habilidadeRepository.buscar("jav", null, PAGE)).hasSize(1);
        assertThat(habilidadeRepository.buscar(null, "backend", PAGE)).hasSize(1);
        assertThat(habilidadeRepository.buscar("ang", "Frontend", PAGE)).hasSize(1);
        assertThat(habilidadeRepository.buscar("ang", "Backend", PAGE)).isEmpty();

        assertThat(habilidadeRepository.listarCategorias()).containsExactly("Backend", "Frontend");
        assertThat(habilidadeRepository.findByNomeIgnoreCase("java")).isPresent();
        assertThat(habilidadeRepository.existsByNomeIgnoreCase("JAVA")).isTrue();
        assertThat(habilidadeRepository.existsByNomeIgnoreCaseAndIdNot("Java", java.getId())).isFalse();

        assertThat(usuarioHabilidadeRepository.countByHabilidade_Id(java.getId())).isZero();
        assertThat(usuarioHabilidadeRepository.countByUsuario_Id(membro.getId())).isZero();
        assertThat(usuarioHabilidadeRepository.existsByUsuario_IdAndHabilidade_Id(
                membro.getId(), java.getId())).isFalse();
    }

    private static Usuario novoUsuario(String email, String nome, NivelAcesso nivel, Curso curso) {
        return Usuario.builder()
                .email(email).nome(nome).senhaHash("hash")
                .nivelAcesso(nivel).curso(curso).ativo(true)
                .build();
    }
}
