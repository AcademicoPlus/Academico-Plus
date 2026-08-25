package com.unifapi.academico.service;


import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.ProjetoHabilidade;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.recomendacao.ProjetoRecomendadoResponse;
import com.unifapi.academico.dto.recomendacao.UsuarioRecomendadoResponse;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CandidaturaRepository;
import com.unifapi.academico.repository.ProjetoHabilidadeRepository;
import com.unifapi.academico.repository.ProjetoMembroRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioHabilidadeRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;


import static com.unifapi.academico.support.EntityFixtures.habilidade;
import static com.unifapi.academico.support.EntityFixtures.nivelAcesso;
import static com.unifapi.academico.support.EntityFixtures.projeto;
import static com.unifapi.academico.support.EntityFixtures.projetoHabilidade;
import static com.unifapi.academico.support.EntityFixtures.usuario;
import static com.unifapi.academico.support.EntityFixtures.usuarioHabilidade;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Testes unitários puros (sem contexto Spring) para {@link RecomendacaoService}.
 * Cobrem as regras de exclusão de candidatos/projetos, a fórmula de
 * compatibilidade ponderada e o clamping do parâmetro de limite.
 */
@ExtendWith(MockitoExtension.class)
class RecomendacaoServiceTest {

    @Mock
    private ProjetoService projetoService;
    @Mock
    private ProjetoRepository projetoRepository;
    @Mock
    private ProjetoHabilidadeRepository projetoHabilidadeRepository;
    @Mock
    private ProjetoMembroRepository projetoMembroRepository;
    @Mock
    private CandidaturaRepository candidaturaRepository;
    @Mock
    private UsuarioHabilidadeRepository usuarioHabilidadeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private RecomendacaoService recomendacaoService;

    private NivelAcesso nivelAcesso;
    private Habilidade java;
    private Habilidade angular;

    @BeforeEach
    void setUp() {
        nivelAcesso = nivelAcesso("ALUNO");
        java = habilidade("Java");
        angular = habilidade("Angular");
    }

    // ============================================
    // recomendarProjetos
    // ============================================

    @Test
    void deveRetornarListaVaziaQuandoUsuarioNaoTemHabilidades() {
        Usuario usuario = usuario("sem-habilidades@teste.com", nivelAcesso);
        given(usuarioHabilidadeRepository.findByUsuario_IdOrderByHabilidade_NomeAsc(usuario.getId()))
                .willReturn(List.of());

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).isEmpty();
        verifyNoInteractions(projetoRepository, projetoMembroRepository, candidaturaRepository);
    }

    @Test
    void deveExcluirProjetoCriadoPeloProprioUsuario() {
        Usuario usuario = usuario("dono@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java);

        Projeto projetoProprio = projeto(usuario, 3);
        projetoProprio.getHabilidadesNecessarias().add(projetoHabilidade(projetoProprio, java, true));

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO)).willReturn(List.of(projetoProprio));

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).isEmpty();
        // Filtro de "criador" é avaliado primeiro na pipeline: os checks abaixo não deveriam ser acionados.
        verifyNoInteractions(projetoMembroRepository, candidaturaRepository);
    }

    @Test
    void deveIncluirProjetoQuandoCriadorForNulo() {
        Usuario usuario = usuario("interessado@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java);

        Projeto projetoSemCriador = projeto(null, 3);
        projetoSemCriador.getHabilidadesNecessarias().add(projetoHabilidade(projetoSemCriador, java, true));

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO)).willReturn(List.of(projetoSemCriador));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projetoSemCriador.getId(), usuario.getId()))
                .willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projetoSemCriador.getId(), usuario.getId()))
                .willReturn(false);

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void deveExcluirProjetoOndeUsuarioJaEhMembro() {
        Usuario usuario = usuario("membro@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java);

        Usuario outroCriador = usuario("criador@teste.com", nivelAcesso);
        Projeto projeto = projeto(outroCriador, 3);
        projeto.getHabilidadesNecessarias().add(projetoHabilidade(projeto, java, true));

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO)).willReturn(List.of(projeto));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), usuario.getId()))
                .willReturn(true);

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).isEmpty();
        verifyNoInteractions(candidaturaRepository);
    }

    @Test
    void deveExcluirProjetoOndeUsuarioJaTemCandidatura() {
        Usuario usuario = usuario("candidato@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java);

        Usuario outroCriador = usuario("criador2@teste.com", nivelAcesso);
        Projeto projeto = projeto(outroCriador, 3);
        projeto.getHabilidadesNecessarias().add(projetoHabilidade(projeto, java, true));

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO)).willReturn(List.of(projeto));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), usuario.getId()))
                .willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), usuario.getId()))
                .willReturn(true);

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveExcluirProjetoSemRequisitosCadastrados() {
        Usuario usuario = usuario("qualquer@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java);

        Usuario outroCriador = usuario("criador3@teste.com", nivelAcesso);
        Projeto projetoSemRequisitos = projeto(outroCriador, 3);
        // Sem nenhum ProjetoHabilidade vinculado: score não tem significado.

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO))
                .willReturn(List.of(projetoSemRequisitos));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveExcluirProjetoComCompatibilidadeZero() {
        Usuario usuario = usuario("java-dev@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java);

        Usuario outroCriador = usuario("criador4@teste.com", nivelAcesso);
        Habilidade python = habilidade("Python");
        Projeto projeto = projeto(outroCriador, 3);
        projeto.getHabilidadesNecessarias().add(projetoHabilidade(projeto, python, true));

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO)).willReturn(List.of(projeto));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveCalcularCompatibilidadeParcialQuandoAtendeApenasObrigatoria() {
        Usuario usuario = usuario("parcial@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java);

        Usuario outroCriador = usuario("criador5@teste.com", nivelAcesso);
        Projeto projeto = projeto(outroCriador, 3);
        projeto.getHabilidadesNecessarias().add(projetoHabilidade(projeto, java, true));
        projeto.getHabilidadesNecessarias().add(projetoHabilidade(projeto, angular, false));

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO)).willReturn(List.of(projeto));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).hasSize(1);
        ProjetoRecomendadoResponse recomendacao = resultado.get(0);
        // obrigatória(peso 2) atendida / total (2 + 1) = 0.6667
        assertThat(recomendacao.compatibilidade()).isCloseTo(2.0 / 3.0, within(0.0001));
        assertThat(recomendacao.habilidadesEmComum()).hasSize(1);
        assertThat(recomendacao.habilidadesEmComum().get(0).id()).isEqualTo(java.getId());
    }

    @Test
    void deveCalcularCompatibilidadeTotalQuandoAtendeTodosOsRequisitos() {
        Usuario usuario = usuario("total@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java, angular);

        Usuario outroCriador = usuario("criador6@teste.com", nivelAcesso);
        Projeto projeto = projeto(outroCriador, 3);
        projeto.getHabilidadesNecessarias().add(projetoHabilidade(projeto, java, true));
        projeto.getHabilidadesNecessarias().add(projetoHabilidade(projeto, angular, false));

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO)).willReturn(List.of(projeto));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).compatibilidade()).isCloseTo(1.0, within(0.0001));
        assertThat(resultado.get(0).habilidadesEmComum()).hasSize(2);
    }

    @Test
    void deveOrdenarProjetosPorCompatibilidadeDescendente() {
        Usuario usuario = usuario("ordenacao@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java);

        Usuario outroCriador = usuario("criador7@teste.com", nivelAcesso);

        // Score 1.0: único requisito (java) atendido.
        Projeto projetoAltaCompatibilidade = projeto(outroCriador, "Projeto Alta", 3, StatusProjeto.ABERTO);
        projetoAltaCompatibilidade.getHabilidadesNecessarias()
                .add(projetoHabilidade(projetoAltaCompatibilidade, java, true));

        // Score 0.5: dois requisitos obrigatórios (peso 4), só um atendido (peso 2).
        Projeto projetoBaixaCompatibilidade = projeto(outroCriador, "Projeto Baixa", 3, StatusProjeto.ABERTO);
        projetoBaixaCompatibilidade.getHabilidadesNecessarias()
                .add(projetoHabilidade(projetoBaixaCompatibilidade, java, true));
        projetoBaixaCompatibilidade.getHabilidadesNecessarias()
                .add(projetoHabilidade(projetoBaixaCompatibilidade, angular, true));

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO))
                .willReturn(List.of(projetoBaixaCompatibilidade, projetoAltaCompatibilidade));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 10);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).projeto().id()).isEqualTo(projetoAltaCompatibilidade.getId());
        assertThat(resultado.get(1).projeto().id()).isEqualTo(projetoBaixaCompatibilidade.getId());
        assertThat(resultado.get(0).compatibilidade()).isGreaterThan(resultado.get(1).compatibilidade());
    }

    @Test
    void deveRespeitarLimiteInformadoRetornandoOsMelhoresPrimeiro() {
        Usuario usuario = usuario("limite@teste.com", nivelAcesso);
        stubHabilidadesDoUsuario(usuario, java, angular);

        Usuario outroCriador = usuario("criador8@teste.com", nivelAcesso);

        Projeto p1 = projeto(outroCriador, "P1", 3, StatusProjeto.ABERTO); // score 1.0
        p1.getHabilidadesNecessarias().add(projetoHabilidade(p1, java, true));

        Projeto p2 = projeto(outroCriador, "P2", 3, StatusProjeto.ABERTO); // score 1.0
        p2.getHabilidadesNecessarias().add(projetoHabilidade(p2, angular, true));

        Habilidade python = habilidade("Python");
        Projeto p3 = projeto(outroCriador, "P3", 3, StatusProjeto.ABERTO); // score 0.5
        p3.getHabilidadesNecessarias().add(projetoHabilidade(p3, java, true));
        p3.getHabilidadesNecessarias().add(projetoHabilidade(p3, python, true));

        given(projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO)).willReturn(List.of(p1, p2, p3));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);

        List<ProjetoRecomendadoResponse> resultado = recomendacaoService.recomendarProjetos(usuario.getId(), 2);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(r -> assertThat(r.compatibilidade()).isEqualTo(1.0));
        assertThat(resultado).extracting(r -> r.projeto().id())
                .containsExactlyInAnyOrder(p1.getId(), p2.getId());
    }

    // ============================================
    // recomendarCandidatos
    // ============================================

    @Test
    void deveLancarExcecaoQuandoUsuarioLogadoNaoEhCriadorDoProjeto() {
        UUID projetoId = UUID.randomUUID();
        UUID usuarioLogadoId = UUID.randomUUID();

        given(projetoService.obterEntidadeComoDono(projetoId, usuarioLogadoId))
                .willThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        assertThatThrownBy(() -> recomendacaoService.recomendarCandidatos(projetoId, usuarioLogadoId, 10))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Você não tem permissão para alterar este projeto.");

        verifyNoInteractions(projetoHabilidadeRepository, usuarioRepository);
    }

    @Test
    void deveRetornarListaVaziaQuandoProjetoNaoTemRequisitos() {
        Usuario criador = usuario("criador9@teste.com", nivelAcesso);
        Projeto projetoSemRequisitos = projeto(criador, 3);

        given(projetoService.obterEntidadeComoDono(projetoSemRequisitos.getId(), criador.getId()))
                .willReturn(projetoSemRequisitos);
        given(projetoHabilidadeRepository.findByProjeto_IdOrderByHabilidade_NomeAsc(projetoSemRequisitos.getId()))
                .willReturn(List.of());

        List<UsuarioRecomendadoResponse> resultado = recomendacaoService.recomendarCandidatos(
                projetoSemRequisitos.getId(), criador.getId(), 10);

        assertThat(resultado).isEmpty();
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveExcluirUsuarioLogadoDaListaDeCandidatos() {
        Usuario criador = usuario("criador10@teste.com", nivelAcesso);
        Projeto projeto = projeto(criador, 3);
        List<ProjetoHabilidade> requisitos = stubRequisitos(projeto, projetoHabilidade(projeto, java, true));

        Usuario candidatoValido = usuario("valido@teste.com", nivelAcesso);
        candidatoValido.adicionarHabilidade(usuarioHabilidade(candidatoValido, java, null));

        given(usuarioRepository.findTodosComHabilidades()).willReturn(List.of(criador, candidatoValido));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), candidatoValido.getId()))
                .willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), candidatoValido.getId()))
                .willReturn(false);

        List<UsuarioRecomendadoResponse> resultado = recomendacaoService.recomendarCandidatos(
                projeto.getId(), criador.getId(), 10);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).usuario().id()).isEqualTo(candidatoValido.getId());
        // O criador é filtrado antes do check de membro/candidatura: nunca deveria disparar essas queries para ele.
        verify(projetoMembroRepository, never()).existsByProjeto_IdAndUsuario_Id(projeto.getId(), criador.getId());
        verify(candidaturaRepository, never()).existsByProjeto_IdAndUsuario_Id(projeto.getId(), criador.getId());
        assertThat(requisitos).isNotEmpty(); // sanity: requisitos foram de fato usados no cálculo
    }

    @Test
    void deveExcluirCandidatosQueJaSaoMembrosDoProjeto() {
        Usuario criador = usuario("criador11@teste.com", nivelAcesso);
        Projeto projeto = projeto(criador, 3);
        stubRequisitos(projeto, projetoHabilidade(projeto, java, true));

        Usuario jaMembro = usuario("membro-existente@teste.com", nivelAcesso);
        jaMembro.adicionarHabilidade(usuarioHabilidade(jaMembro, java, null));

        Usuario candidatoNovo = usuario("novo@teste.com", nivelAcesso);
        candidatoNovo.adicionarHabilidade(usuarioHabilidade(candidatoNovo, java, null));

        given(usuarioRepository.findTodosComHabilidades()).willReturn(List.of(jaMembro, candidatoNovo));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), jaMembro.getId()))
                .willReturn(true);
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), candidatoNovo.getId()))
                .willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), candidatoNovo.getId()))
                .willReturn(false);

        List<UsuarioRecomendadoResponse> resultado = recomendacaoService.recomendarCandidatos(
                projeto.getId(), criador.getId(), 10);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).usuario().id()).isEqualTo(candidatoNovo.getId());
    }

    @Test
    void deveExcluirCandidatosComCandidaturaJaExistente() {
        Usuario criador = usuario("criador12@teste.com", nivelAcesso);
        Projeto projeto = projeto(criador, 3);
        stubRequisitos(projeto, projetoHabilidade(projeto, java, true));

        Usuario jaCandidatou = usuario("ja-candidatou@teste.com", nivelAcesso);
        jaCandidatou.adicionarHabilidade(usuarioHabilidade(jaCandidatou, java, null));

        Usuario candidatoNovo = usuario("novo2@teste.com", nivelAcesso);
        candidatoNovo.adicionarHabilidade(usuarioHabilidade(candidatoNovo, java, null));

        given(usuarioRepository.findTodosComHabilidades()).willReturn(List.of(jaCandidatou, candidatoNovo));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(eq(projeto.getId()), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), jaCandidatou.getId()))
                .willReturn(true);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), candidatoNovo.getId()))
                .willReturn(false);

        List<UsuarioRecomendadoResponse> resultado = recomendacaoService.recomendarCandidatos(
                projeto.getId(), criador.getId(), 10);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).usuario().id()).isEqualTo(candidatoNovo.getId());
    }

    @Test
    void deveCalcularCompatibilidadeDoCandidatoComBaseNasHabilidades() {
        Usuario criador = usuario("criador13@teste.com", nivelAcesso);
        Projeto projeto = projeto(criador, 3);
        stubRequisitos(projeto,
                projetoHabilidade(projeto, java, true),
                projetoHabilidade(projeto, angular, false));

        Usuario candidato = usuario("candidato-score@teste.com", nivelAcesso);
        candidato.adicionarHabilidade(usuarioHabilidade(candidato, java, null));

        given(usuarioRepository.findTodosComHabilidades()).willReturn(List.of(candidato));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);

        List<UsuarioRecomendadoResponse> resultado = recomendacaoService.recomendarCandidatos(
                projeto.getId(), criador.getId(), 10);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).compatibilidade()).isCloseTo(2.0 / 3.0, within(0.0001));
        assertThat(resultado.get(0).habilidadesEmComum()).hasSize(1);
        assertThat(resultado.get(0).habilidadesEmComum().get(0).id()).isEqualTo(java.getId());
    }

    @Test
    void deveExcluirCandidatoComCompatibilidadeZero() {
        Usuario criador = usuario("criador14@teste.com", nivelAcesso);
        Projeto projeto = projeto(criador, 3);
        Habilidade python = habilidade("Python");
        stubRequisitos(projeto, projetoHabilidade(projeto, python, true));

        Usuario candidatoSemHabilidadeCompativel = usuario("sem-match@teste.com", nivelAcesso);
        candidatoSemHabilidadeCompativel.adicionarHabilidade(usuarioHabilidade(candidatoSemHabilidadeCompativel, java, null));

        given(usuarioRepository.findTodosComHabilidades()).willReturn(List.of(candidatoSemHabilidadeCompativel));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);

        List<UsuarioRecomendadoResponse> resultado = recomendacaoService.recomendarCandidatos(
                projeto.getId(), criador.getId(), 10);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveOrdenarCandidatosPorCompatibilidadeDescendente() {
        Usuario criador = usuario("criador15@teste.com", nivelAcesso);
        Projeto projeto = projeto(criador, 3);
        stubRequisitos(projeto,
                projetoHabilidade(projeto, java, true),
                projetoHabilidade(projeto, angular, true));

        Usuario candidatoCompleto = usuario("completo@teste.com", nivelAcesso);
        candidatoCompleto.adicionarHabilidade(usuarioHabilidade(candidatoCompleto, java, null));
        candidatoCompleto.adicionarHabilidade(usuarioHabilidade(candidatoCompleto, angular, null));

        Usuario candidatoParcial = usuario("parcial-cand@teste.com", nivelAcesso);
        candidatoParcial.adicionarHabilidade(usuarioHabilidade(candidatoParcial, java, null));

        given(usuarioRepository.findTodosComHabilidades())
                .willReturn(List.of(candidatoParcial, candidatoCompleto));
        given(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);
        given(candidaturaRepository.existsByProjeto_IdAndUsuario_Id(any(), any())).willReturn(false);

        List<UsuarioRecomendadoResponse> resultado = recomendacaoService.recomendarCandidatos(
                projeto.getId(), criador.getId(), 10);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).usuario().id()).isEqualTo(candidatoCompleto.getId());
        assertThat(resultado.get(1).usuario().id()).isEqualTo(candidatoParcial.getId());
        assertThat(resultado.get(0).compatibilidade()).isGreaterThan(resultado.get(1).compatibilidade());
    }

    // ============================================
    // limitar (helper privado de clamping do limite)
    // ============================================

    @Test
    void deveUsarLimitePadraoQuandoLimiteForNulo() throws Exception {
        assertThat(invocarLimitar(null)).isEqualTo(10);
    }

    @Test
    void deveUsarLimitePadraoQuandoLimiteForZero() throws Exception {
        assertThat(invocarLimitar(0)).isEqualTo(10);
    }

    @Test
    void deveUsarLimitePadraoQuandoLimiteForNegativo() throws Exception {
        assertThat(invocarLimitar(-5)).isEqualTo(10);
    }

    @Test
    void deveClampearLimiteAoMaximoQuandoAcimaDoPermitido() throws Exception {
        assertThat(invocarLimitar(1000)).isEqualTo(50);
    }

    @Test
    void deveManterLimiteInformadoQuandoDentroDoIntervaloPermitido() throws Exception {
        assertThat(invocarLimitar(5)).isEqualTo(5);
    }

    // ============================================
    // Helpers de fixture/reflexão
    // ============================================

    private void stubHabilidadesDoUsuario(Usuario usuario, Habilidade... habilidades) {
        List<com.unifapi.academico.domain.models.UsuarioHabilidade> vinculos = List.of(habilidades).stream()
                .map(h -> usuarioHabilidade(usuario, h, null))
                .toList();
        given(usuarioHabilidadeRepository.findByUsuario_IdOrderByHabilidade_NomeAsc(usuario.getId()))
                .willReturn(vinculos);
    }

    private List<ProjetoHabilidade> stubRequisitos(Projeto projeto, ProjetoHabilidade... requisitos) {
        List<ProjetoHabilidade> lista = List.of(requisitos);
        given(projetoService.obterEntidadeComoDono(projeto.getId(), projeto.getCriador().getId()))
                .willReturn(projeto);
        given(projetoHabilidadeRepository.findByProjeto_IdOrderByHabilidade_NomeAsc(projeto.getId()))
                .willReturn(lista);
        return lista;
    }

    /** {@code limitar} é privado: acessado via reflexão para testar o clamping isoladamente. */
    private int invocarLimitar(Integer limite) throws Exception {
        Method metodo = RecomendacaoService.class.getDeclaredMethod("limitar", Integer.class);
        metodo.setAccessible(true);
        return (int) metodo.invoke(recomendacaoService, limite);
    }
}
