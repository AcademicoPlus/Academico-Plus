package com.unifapi.academico.repository;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.*;
import com.unifapi.academico.dto.avaliacao.AvaliarRequest;
import com.unifapi.academico.dto.recomendacao.ProjetoRecomendadoResponse;
import com.unifapi.academico.dto.recomendacao.UsuarioRecomendadoResponse;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.service.AvaliacaoService;
import com.unifapi.academico.service.HabilidadeService;
import com.unifapi.academico.service.ProjetoService;
import com.unifapi.academico.service.RecomendacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Teste descartável: exercita as regras de negócio de Avaliacao e a fórmula
 * de compatibilidade de RecomendacaoService contra H2. Os services são
 * instanciados manualmente (não são beans do @DataJpaTest), passando os
 * repositories reais fornecidos pelo contexto de teste.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:smoke2;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class AvaliacaoRecomendacaoSmokeTest {

    @Autowired private TestEntityManager em;
    @Autowired private AvaliacaoRepository avaliacaoRepository;
    @Autowired private ProjetoRepository projetoRepository;
    @Autowired private ProjetoMembroRepository projetoMembroRepository;
    @Autowired private ProjetoHabilidadeRepository projetoHabilidadeRepository;
    @Autowired private CandidaturaRepository candidaturaRepository;
    @Autowired private HabilidadeRepository habilidadeRepository;
    @Autowired private UsuarioHabilidadeRepository usuarioHabilidadeRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProjetoCanceladoAvisoRepository projetoCanceladoAvisoRepository;

    private AvaliacaoService avaliacaoService;
    private RecomendacaoService recomendacaoService;

    private Usuario criador;
    private Usuario membro1;      // java + react — cobre as 2 habilidades obrigatórias
    private Usuario membro2;      // docker — só a desejável
    private Usuario candidatoExterno; // java + react, ainda fora do projeto
    private Usuario semHabilidade;
    private Projeto projeto;

    @BeforeEach
    void fixture() {
        HabilidadeService habilidadeService = new HabilidadeService(
                habilidadeRepository, projetoHabilidadeRepository, usuarioHabilidadeRepository);
        ProjetoService projetoService = new ProjetoService(
                projetoRepository, usuarioRepository, projetoMembroRepository, candidaturaRepository,
                projetoHabilidadeRepository, habilidadeRepository, projetoCanceladoAvisoRepository, habilidadeService);
        avaliacaoService = new AvaliacaoService(avaliacaoRepository, projetoRepository, projetoMembroRepository, usuarioRepository);
        recomendacaoService = new RecomendacaoService(
                projetoService, projetoRepository, projetoHabilidadeRepository, projetoMembroRepository,
                candidaturaRepository, usuarioHabilidadeRepository, usuarioRepository);

        NivelAcesso aluno = em.persist(NivelAcesso.builder().nome("ALUNO").build());
        Curso curso = em.persist(Curso.builder().nome("Ciencia da Computacao").sigla("CC").build());

        criador = em.persist(novoUsuario("criador@unifapi.edu.br", "Criador", aluno, curso));
        membro1 = em.persist(novoUsuario("membro1@unifapi.edu.br", "Membro Um", aluno, curso));
        membro2 = em.persist(novoUsuario("membro2@unifapi.edu.br", "Membro Dois", aluno, curso));
        candidatoExterno = em.persist(novoUsuario("externo@unifapi.edu.br", "Candidato Externo", aluno, curso));
        semHabilidade = em.persist(novoUsuario("semhabilidade@unifapi.edu.br", "Sem Habilidade", aluno, curso));

        Habilidade java = em.persist(Habilidade.builder().nome("Java").categoria("Backend").build());
        Habilidade react = em.persist(Habilidade.builder().nome("React").categoria("Frontend").build());
        Habilidade docker = em.persist(Habilidade.builder().nome("Docker").categoria("DevOps").build());

        projeto = em.persist(Projeto.builder()
                .criador(criador)
                .titulo("Plataforma de estagios academicos")
                .descricao("Projeto para conectar alunos a vagas de estagio na regiao.")
                .status(StatusProjeto.ABERTO)
                .vagas(3)
                .vagasPreenchidas(1)
                .ativo(true)
                .build());

        em.persist(ProjetoHabilidade.builder().projeto(projeto).habilidade(java).obrigatoria(true).build());
        em.persist(ProjetoHabilidade.builder().projeto(projeto).habilidade(react).obrigatoria(true).build());
        em.persist(ProjetoHabilidade.builder().projeto(projeto).habilidade(docker).obrigatoria(false).build());

        em.persist(ProjetoMembro.builder().projeto(projeto).usuario(membro1).dataAdesao(LocalDateTime.now()).build());

        em.persist(UsuarioHabilidade.builder().usuario(membro1).habilidade(java).build());
        em.persist(UsuarioHabilidade.builder().usuario(membro1).habilidade(react).build());
        em.persist(UsuarioHabilidade.builder().usuario(membro2).habilidade(docker).build());
        em.persist(UsuarioHabilidade.builder().usuario(candidatoExterno).habilidade(java).build());
        em.persist(UsuarioHabilidade.builder().usuario(candidatoExterno).habilidade(react).build());

        em.flush();
        em.clear();
        // Reanexa as referências usadas depois do clear, para os métodos de negócio operarem no estado persistido.
        projeto = em.find(Projeto.class, projeto.getId());
    }

    @Test
    void naoPermiteAvaliarEnquantoProjetoEstaAberto() {
        AvaliarRequest request = new AvaliarRequest(membro1.getId(), 5, "Otimo colega");

        assertThatThrownBy(() -> avaliacaoService.avaliar(projeto.getId(), request, criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("concluído ou cancelado");
    }

    @Test
    void aplicaRegrasDeNegocioDaAvaliacao() {
        projeto.setStatus(StatusProjeto.CONCLUIDO);
        em.persist(projeto);
        em.flush();

        // Não pode avaliar a si mesmo.
        assertThatThrownBy(() -> avaliacaoService.avaliar(
                projeto.getId(), new AvaliarRequest(criador.getId(), 5, null), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("a si mesmo");

        // Quem não participou não pode avaliar.
        assertThatThrownBy(() -> avaliacaoService.avaliar(
                projeto.getId(), new AvaliarRequest(membro1.getId(), 5, null), candidatoExterno.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não participou");

        // Não pode avaliar quem não participou do projeto.
        assertThatThrownBy(() -> avaliacaoService.avaliar(
                projeto.getId(), new AvaliarRequest(candidatoExterno.getId(), 5, null), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não participou");

        // Caminho feliz: criador avalia membro1.
        avaliacaoService.avaliar(projeto.getId(), new AvaliarRequest(membro1.getId(), 4, "Bom trabalho"), criador.getId());

        Usuario membro1Atualizado = usuarioRepository.findById(membro1.getId()).orElseThrow();
        assertThat(membro1Atualizado.getNotaMedia()).isCloseTo(4.0, within(0.001));
        assertThat(membro1Atualizado.getTotalAvaliacoes()).isEqualTo(1);

        // Não pode avaliar a mesma pessoa duas vezes no mesmo projeto.
        assertThatThrownBy(() -> avaliacaoService.avaliar(
                projeto.getId(), new AvaliarRequest(membro1.getId(), 3, null), criador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já avaliou");
    }

    @Test
    void recomendaProjetosPorCoberturaPonderadaDeHabilidades() {
        // candidatoExterno cobre as 2 obrigatórias (java, react) mas não a desejável (docker):
        // peso atendido = 2+2 = 4, peso total = 2+2+1 = 5 -> 0.8
        List<ProjetoRecomendadoResponse> recomendados =
                recomendacaoService.recomendarProjetos(candidatoExterno.getId(), null);

        assertThat(recomendados).hasSize(1);
        ProjetoRecomendadoResponse recomendado = recomendados.get(0);
        assertThat(recomendado.projeto().id()).isEqualTo(projeto.getId());
        assertThat(recomendado.compatibilidade()).isCloseTo(0.8, within(0.001));
        assertThat(recomendado.habilidadesEmComum()).extracting(h -> h.nome())
                .containsExactlyInAnyOrder("Java", "React");

        // Usuário sem nenhuma habilidade cadastrada não recebe recomendação.
        assertThat(recomendacaoService.recomendarProjetos(semHabilidade.getId(), null)).isEmpty();

        // Quem já é membro do projeto não recebe ele como recomendação.
        assertThat(recomendacaoService.recomendarProjetos(membro1.getId(), null)).isEmpty();
    }

    @Test
    void recomendaCandidatosPorCoberturaPonderadaDeHabilidades() {
        List<UsuarioRecomendadoResponse> recomendados =
                recomendacaoService.recomendarCandidatos(projeto.getId(), criador.getId(), null);

        // membro1 já é membro do projeto: não deve aparecer como candidato.
        List<UUID> ids = recomendados.stream().map(r -> r.usuario().id()).collect(Collectors.toList());
        assertThat(ids).doesNotContain(membro1.getId());

        assertThat(recomendados).hasSize(2);

        UsuarioRecomendadoResponse primeiro = recomendados.get(0);
        assertThat(primeiro.usuario().id()).isEqualTo(candidatoExterno.getId());
        assertThat(primeiro.compatibilidade()).isCloseTo(0.8, within(0.001));

        UsuarioRecomendadoResponse segundo = recomendados.get(1);
        assertThat(segundo.usuario().id()).isEqualTo(membro2.getId());
        assertThat(segundo.compatibilidade()).isCloseTo(0.2, within(0.001));
    }

    @Test
    void somenteCriadorVeRecomendacaoDeCandidatos() {
        assertThatThrownBy(() -> recomendacaoService.recomendarCandidatos(
                projeto.getId(), candidatoExterno.getId(), null))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    private static Usuario novoUsuario(String email, String nome, NivelAcesso nivel, Curso curso) {
        return Usuario.builder()
                .email(email).nome(nome).senhaHash("hash")
                .nivelAcesso(nivel).curso(curso).ativo(true)
                .build();
    }
}
