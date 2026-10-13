package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.CategoriaDenuncia;
import com.unifapi.academico.domain.enums.MedidaModeracao;
import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.enums.TipoAlvoDenuncia;
import com.unifapi.academico.domain.models.Avaliacao;
import com.unifapi.academico.domain.models.Denuncia;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Sancao;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.denuncia.DenunciaResponse;
import com.unifapi.academico.dto.denuncia.DenunciarRequest;
import com.unifapi.academico.dto.denuncia.ResolverDenunciaRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.AvaliacaoRepository;
import com.unifapi.academico.repository.DenunciaRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.SancaoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.support.EntityFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários de {@link DenunciaService}, com colaboradores mockados
 * (sem contexto Spring). Cobre quem pode denunciar cada tipo de alvo e a
 * escada de sanções aplicada na resolução.
 */
@ExtendWith(MockitoExtension.class)
class DenunciaServiceTest {

    private static final String MOTIVO = "Motivo qualquer com mais de dez chars";

    @Mock
    private DenunciaRepository denunciaRepository;

    @Mock
    private SancaoRepository sancaoRepository;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AvaliacaoService avaliacaoService;

    @Mock
    private ProjetoService projetoService;

    @InjectMocks
    private DenunciaService denunciaService;

    private Usuario avaliador;
    private Usuario avaliado;
    private Usuario terceiro;
    private Usuario admin;
    private Projeto projeto;
    private Avaliacao avaliacao;

    @BeforeEach
    void setUp() {
        NivelAcesso nivelUsuario = EntityFixtures.nivelAcesso("USER");
        NivelAcesso nivelAdmin = EntityFixtures.nivelAcesso("ADMIN");

        avaliador = EntityFixtures.usuario("avaliador@teste.com", nivelUsuario);
        avaliado = EntityFixtures.usuario("avaliado@teste.com", nivelUsuario);
        terceiro = EntityFixtures.usuario("terceiro@teste.com", nivelUsuario);
        admin = EntityFixtures.usuario("admin@teste.com", nivelAdmin);

        projeto = EntityFixtures.projeto(avaliador, 3);
        avaliacao = EntityFixtures.avaliacao(projeto, avaliador, avaliado, 2);
    }

    private static DenunciarRequest pedido(String motivo) {
        return new DenunciarRequest(CategoriaDenuncia.CONTEUDO_OFENSIVO, motivo);
    }

    // ===============================================================
    // DENÚNCIA DE AVALIAÇÃO (regras que já valiam antes da escada)
    // ===============================================================

    @Nested
    class DenunciaDeAvaliacao {

        @Test
        void deveLancarExcecaoQuandoAvaliacaoNaoEncontrada() {
            UUID avaliacaoId = UUID.randomUUID();
            when(avaliacaoRepository.findById(avaliacaoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> denunciaService.denunciarAvaliacao(avaliacaoId, pedido(MOTIVO), avaliado.getId()))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Avaliação não encontrada.");

            verifyNoInteractions(denunciaRepository);
            verifyNoInteractions(usuarioRepository);
        }

        @Test
        void deveLancarExcecaoQuandoAvaliacaoNaoTemAvaliadoDefinido() {
            Avaliacao semAvaliado = Avaliacao.builder()
                    .id(UUID.randomUUID())
                    .projeto(projeto)
                    .avaliador(avaliador)
                    .avaliado(null)
                    .nota(3)
                    .removida(false)
                    .criadoEm(LocalDateTime.now())
                    .build();
            when(avaliacaoRepository.findById(semAvaliado.getId())).thenReturn(Optional.of(semAvaliado));

            assertThatThrownBy(() ->
                    denunciaService.denunciarAvaliacao(semAvaliado.getId(), pedido(MOTIVO), avaliado.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Você só pode denunciar avaliações recebidas por você.");

            verifyNoInteractions(denunciaRepository);
        }

        @Test
        void deveLancarExcecaoQuandoAvaliadorTentaDenunciarAvaliacaoQueEleProprioEscreveu() {
            when(avaliacaoRepository.findById(avaliacao.getId())).thenReturn(Optional.of(avaliacao));

            assertThatThrownBy(() ->
                    denunciaService.denunciarAvaliacao(avaliacao.getId(), pedido(MOTIVO), avaliador.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Você só pode denunciar avaliações recebidas por você.");

            verifyNoInteractions(denunciaRepository);
        }

        @Test
        void deveLancarExcecaoQuandoDenuncianteForTerceiroNaoRelacionado() {
            when(avaliacaoRepository.findById(avaliacao.getId())).thenReturn(Optional.of(avaliacao));

            assertThatThrownBy(() ->
                    denunciaService.denunciarAvaliacao(avaliacao.getId(), pedido(MOTIVO), terceiro.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Você só pode denunciar avaliações recebidas por você.");

            verifyNoInteractions(denunciaRepository);
        }

        @Test
        void deveLancarExcecaoQuandoJaDenunciouAMesmaAvaliacao() {
            when(avaliacaoRepository.findById(avaliacao.getId())).thenReturn(Optional.of(avaliacao));
            when(denunciaRepository.existsByAvaliacao_IdAndDenunciante_Id(avaliacao.getId(), avaliado.getId()))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    denunciaService.denunciarAvaliacao(avaliacao.getId(), pedido(MOTIVO), avaliado.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Você já denunciou esta avaliação.");

            verifyNoInteractions(usuarioRepository);
            verify(denunciaRepository, never()).save(any());
        }

        @Test
        void devePermitirQuePessoaAvaliadaDenuncieAAvaliacaoRecebida() {
            when(avaliacaoRepository.findById(avaliacao.getId())).thenReturn(Optional.of(avaliacao));
            when(denunciaRepository.existsByAvaliacao_IdAndDenunciante_Id(avaliacao.getId(), avaliado.getId()))
                    .thenReturn(false);
            when(usuarioRepository.getReferenceById(avaliado.getId())).thenReturn(avaliado);
            when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(i -> i.getArgument(0));

            DenunciaResponse response = denunciaService.denunciarAvaliacao(
                    avaliacao.getId(),
                    pedido("   Comentário ofensivo e sem relação com o projeto.   "),
                    avaliado.getId());

            ArgumentCaptor<Denuncia> captor = ArgumentCaptor.forClass(Denuncia.class);
            verify(denunciaRepository).save(captor.capture());

            Denuncia salva = captor.getValue();
            assertThat(salva.getTipoAlvo()).isEqualTo(TipoAlvoDenuncia.AVALIACAO);
            assertThat(salva.getStatus()).isEqualTo(StatusDenuncia.PENDENTE);
            assertThat(salva.getMotivo()).isEqualTo("Comentário ofensivo e sem relação com o projeto.");
            assertThat(salva.getAvaliacao()).isEqualTo(avaliacao);
            assertThat(salva.getDenunciante()).isEqualTo(avaliado);

            assertThat(response.status()).isEqualTo(StatusDenuncia.PENDENTE);
            assertThat(response.alvo().id()).isEqualTo(avaliacao.getId());
            // O responsável por uma avaliação abusiva é quem a escreveu.
            assertThat(response.responsavel().id()).isEqualTo(avaliador.getId());
        }
    }

    // ===============================================================
    // DENÚNCIA DE PROJETO
    // ===============================================================

    @Nested
    class DenunciaDeProjeto {

        @Test
        void deveLancarExcecaoQuandoProjetoNaoEncontrado() {
            UUID projetoId = UUID.randomUUID();
            when(projetoRepository.findById(projetoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> denunciaService.denunciarProjeto(projetoId, pedido(MOTIVO), terceiro.getId()))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Projeto não encontrado.");

            verifyNoInteractions(denunciaRepository);
        }

        @Test
        void deveImpedirQueOCriadorDenuncieOProprioProjeto() {
            when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));

            assertThatThrownBy(() ->
                    denunciaService.denunciarProjeto(projeto.getId(), pedido(MOTIVO), avaliador.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Você não pode denunciar um projeto seu.");

            verify(denunciaRepository, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoJaDenunciouOMesmoProjeto() {
            when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
            when(denunciaRepository.existsByProjeto_IdAndDenunciante_Id(projeto.getId(), terceiro.getId()))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    denunciaService.denunciarProjeto(projeto.getId(), pedido(MOTIVO), terceiro.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Você já denunciou este projeto.");

            verify(denunciaRepository, never()).save(any());
        }

        @Test
        void devePermitirQueQualquerOutroUsuarioDenuncieOProjeto() {
            when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));
            when(denunciaRepository.existsByProjeto_IdAndDenunciante_Id(projeto.getId(), terceiro.getId()))
                    .thenReturn(false);
            when(usuarioRepository.getReferenceById(terceiro.getId())).thenReturn(terceiro);
            when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(i -> i.getArgument(0));

            DenunciaResponse response =
                    denunciaService.denunciarProjeto(projeto.getId(), pedido(MOTIVO), terceiro.getId());

            assertThat(response.tipoAlvo()).isEqualTo(TipoAlvoDenuncia.PROJETO);
            assertThat(response.alvo().id()).isEqualTo(projeto.getId());
            // Quem responde por um projeto denunciado é seu criador.
            assertThat(response.responsavel().id()).isEqualTo(avaliador.getId());
        }
    }

    // ===============================================================
    // DENÚNCIA DE PERFIL
    // ===============================================================

    @Nested
    class DenunciaDePerfil {

        @Test
        void deveImpedirAutodenuncia() {
            assertThatThrownBy(() ->
                    denunciaService.denunciarPerfil(terceiro.getId(), pedido(MOTIVO), terceiro.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Você não pode denunciar o próprio perfil.");

            verifyNoInteractions(denunciaRepository);
            verifyNoInteractions(usuarioRepository);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioDenunciadoNaoExiste() {
            UUID inexistente = UUID.randomUUID();
            when(usuarioRepository.findById(inexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    denunciaService.denunciarPerfil(inexistente, pedido(MOTIVO), terceiro.getId()))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Usuário não encontrado.");

            verifyNoInteractions(denunciaRepository);
        }

        @Test
        void deveLancarExcecaoQuandoJaDenunciouOMesmoPerfil() {
            when(usuarioRepository.findById(avaliador.getId())).thenReturn(Optional.of(avaliador));
            when(denunciaRepository.existsByUsuarioDenunciado_IdAndDenunciante_Id(
                    avaliador.getId(), terceiro.getId())).thenReturn(true);

            assertThatThrownBy(() ->
                    denunciaService.denunciarPerfil(avaliador.getId(), pedido(MOTIVO), terceiro.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Você já denunciou este perfil.");

            verify(denunciaRepository, never()).save(any());
        }

        @Test
        void devePermitirDenunciarOPerfilDeOutroUsuario() {
            when(usuarioRepository.findById(avaliador.getId())).thenReturn(Optional.of(avaliador));
            when(denunciaRepository.existsByUsuarioDenunciado_IdAndDenunciante_Id(
                    avaliador.getId(), terceiro.getId())).thenReturn(false);
            when(usuarioRepository.getReferenceById(terceiro.getId())).thenReturn(terceiro);
            when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(i -> i.getArgument(0));

            DenunciaResponse response =
                    denunciaService.denunciarPerfil(avaliador.getId(), pedido(MOTIVO), terceiro.getId());

            assertThat(response.tipoAlvo()).isEqualTo(TipoAlvoDenuncia.PERFIL);
            assertThat(response.alvo().id()).isEqualTo(avaliador.getId());
            assertThat(response.responsavel().id()).isEqualTo(avaliador.getId());
        }
    }

    // ===============================================================
    // FILA DO ADMIN: histórico e sugestão da escada
    // ===============================================================

    @Nested
    class FilaDeModeracao {

        @Test
        void deveSugerirAdvertenciaQuandoResponsavelNaoTemHistorico() {
            Pageable pageable = PageRequest.of(0, 10);
            Denuncia denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Motivo registrado na denúncia");
            when(denunciaRepository.findByStatusOrderByCriadoEmAsc(StatusDenuncia.PENDENTE, pageable))
                    .thenReturn(new PageImpl<>(List.of(denuncia), pageable, 1));
            when(sancaoRepository.countByUsuario_IdAndMedida(avaliador.getId(), MedidaModeracao.ADVERTENCIA))
                    .thenReturn(0L);
            when(sancaoRepository.countByUsuario_IdAndMedida(avaliador.getId(), MedidaModeracao.SUSPENSAO))
                    .thenReturn(0L);

            Page<DenunciaResponse> resultado = denunciaService.listarPendentes(null, pageable);

            assertThat(resultado.getContent()).hasSize(1);
            var historico = resultado.getContent().get(0).historicoResponsavel();
            assertThat(historico.advertencias()).isZero();
            assertThat(historico.medidaSugerida()).isEqualTo(MedidaModeracao.ADVERTENCIA);
            assertThat(historico.suspensoAgora()).isFalse();
        }

        @Test
        void deveSugerirSuspensaoQuandoResponsavelJaAcumulouDuasAdvertencias() {
            Pageable pageable = PageRequest.of(0, 10);
            Denuncia denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Reincidência");
            when(denunciaRepository.findByStatusOrderByCriadoEmAsc(StatusDenuncia.PENDENTE, pageable))
                    .thenReturn(new PageImpl<>(List.of(denuncia), pageable, 1));
            when(sancaoRepository.countByUsuario_IdAndMedida(avaliador.getId(), MedidaModeracao.ADVERTENCIA))
                    .thenReturn(2L);
            when(sancaoRepository.countByUsuario_IdAndMedida(avaliador.getId(), MedidaModeracao.SUSPENSAO))
                    .thenReturn(0L);

            var historico = denunciaService.listarPendentes(null, pageable)
                    .getContent().get(0).historicoResponsavel();

            assertThat(historico.advertencias()).isEqualTo(2);
            assertThat(historico.medidaSugerida()).isEqualTo(MedidaModeracao.SUSPENSAO);
            assertThat(historico.justificativaSugestao()).contains("recomenda suspender");
        }

        @Test
        void deveFiltrarPorTipoDeAlvoQuandoInformado() {
            Pageable pageable = PageRequest.of(0, 10);
            Denuncia denuncia = EntityFixtures.denunciaDePerfil(avaliador, terceiro, "Perfil com golpe");
            when(denunciaRepository.findByStatusAndTipoAlvoOrderByCriadoEmAsc(
                    StatusDenuncia.PENDENTE, TipoAlvoDenuncia.PERFIL, pageable))
                    .thenReturn(new PageImpl<>(List.of(denuncia), pageable, 1));

            Page<DenunciaResponse> resultado =
                    denunciaService.listarPendentes(TipoAlvoDenuncia.PERFIL, pageable);

            assertThat(resultado.getContent()).hasSize(1);
            verify(denunciaRepository).findByStatusAndTipoAlvoOrderByCriadoEmAsc(
                    eq(StatusDenuncia.PENDENTE), eq(TipoAlvoDenuncia.PERFIL), eq(pageable));
            verify(denunciaRepository, never()).findByStatusOrderByCriadoEmAsc(any(), any());
        }
    }

    // ===============================================================
    // RESOLUÇÃO: a escada de medidas
    // ===============================================================

    @Nested
    class Resolucao {

        private ResolverDenunciaRequest pedidoDeResolucao(MedidaModeracao medida, Integer dias, String justificativa) {
            return new ResolverDenunciaRequest(medida, dias, justificativa);
        }

        @Test
        void deveLancarExcecaoQuandoDenunciaNaoEncontrada() {
            UUID denunciaId = UUID.randomUUID();
            when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> denunciaService.resolver(
                    denunciaId, pedidoDeResolucao(MedidaModeracao.ARQUIVAR, null, null), admin.getId()))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Denúncia não encontrada.");

            verifyNoInteractions(avaliacaoRepository);
            verifyNoInteractions(sancaoRepository);
        }

        @Test
        void deveLancarExcecaoQuandoDenunciaJaFoiAnalisada() {
            Denuncia denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Já analisada");
            denuncia.setStatus(StatusDenuncia.PROCEDENTE);
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));

            assertThatThrownBy(() -> denunciaService.resolver(
                    denuncia.getId(), pedidoDeResolucao(MedidaModeracao.ARQUIVAR, null, null), admin.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Esta denúncia já foi analisada.");

            verifyNoInteractions(avaliacaoRepository);
            verifyNoInteractions(sancaoRepository);
        }

        @Test
        void arquivarDeveMarcarImprocedenteSemTocarNoConteudoNemSancionar() {
            Denuncia denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Não procede");
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));
            when(usuarioRepository.getReferenceById(admin.getId())).thenReturn(admin);

            DenunciaResponse response = denunciaService.resolver(
                    denuncia.getId(), pedidoDeResolucao(MedidaModeracao.ARQUIVAR, null, null), admin.getId());

            assertThat(response.status()).isEqualTo(StatusDenuncia.IMPROCEDENTE);
            assertThat(denuncia.getMedida()).isEqualTo(MedidaModeracao.ARQUIVAR);
            assertThat(denuncia.getAnalisadoPor()).isEqualTo(admin);
            assertThat(avaliacao.getRemovida()).isFalse();

            verify(avaliacaoRepository, never()).save(any());
            verify(sancaoRepository, never()).save(any());
        }

        @Test
        void removerConteudoDeveApagarAvaliacaoERecalcularMediaSemSancionar() {
            Denuncia denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Comentário abusivo");
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));
            when(usuarioRepository.getReferenceById(admin.getId())).thenReturn(admin);

            DenunciaResponse response = denunciaService.resolver(
                    denuncia.getId(), pedidoDeResolucao(MedidaModeracao.REMOVER_CONTEUDO, null, null), admin.getId());

            assertThat(response.status()).isEqualTo(StatusDenuncia.PROCEDENTE);
            assertThat(avaliacao.getRemovida()).isTrue();

            verify(avaliacaoRepository).save(avaliacao);
            verify(avaliacaoRepository).flush();
            verify(avaliacaoService, times(1)).recalcularMedia(avaliado.getId());
            verify(sancaoRepository, never()).save(any());
        }

        @Test
        void advertenciaDeveRegistrarSancaoAoResponsavelEDerrubarOConteudo() {
            Denuncia denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Comentário abusivo");
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));
            when(usuarioRepository.getReferenceById(admin.getId())).thenReturn(admin);

            denunciaService.resolver(
                    denuncia.getId(),
                    pedidoDeResolucao(MedidaModeracao.ADVERTENCIA, null, "  Comentário ofensivo a colega.  "),
                    admin.getId());

            ArgumentCaptor<Sancao> captor = ArgumentCaptor.forClass(Sancao.class);
            verify(sancaoRepository).save(captor.capture());

            Sancao sancao = captor.getValue();
            assertThat(sancao.getMedida()).isEqualTo(MedidaModeracao.ADVERTENCIA);
            // Quem é advertido é o autor da avaliação, não quem a recebeu e denunciou.
            assertThat(sancao.getUsuario()).isEqualTo(avaliador);
            assertThat(sancao.getJustificativa()).isEqualTo("Comentário ofensivo a colega.");
            assertThat(sancao.getExpiraEm()).isNull();
            assertThat(sancao.getDenuncia()).isEqualTo(denuncia);

            // Advertência é cumulativa com a remoção do conteúdo.
            assertThat(avaliacao.getRemovida()).isTrue();
            // Advertência não bloqueia o acesso.
            assertThat(avaliador.getSuspensoAte()).isNull();
        }

        @Test
        void suspensaoDeveGravarPrazoNoUsuarioENaSancao() {
            Denuncia denuncia = EntityFixtures.denunciaDePerfil(avaliador, terceiro, "Perfil com golpe");
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));
            when(usuarioRepository.getReferenceById(admin.getId())).thenReturn(admin);

            LocalDateTime antes = LocalDateTime.now();

            denunciaService.resolver(
                    denuncia.getId(),
                    pedidoDeResolucao(MedidaModeracao.SUSPENSAO, 7, "Divulgação de golpe no perfil."),
                    admin.getId());

            ArgumentCaptor<Sancao> captor = ArgumentCaptor.forClass(Sancao.class);
            verify(sancaoRepository).save(captor.capture());

            Sancao sancao = captor.getValue();
            assertThat(sancao.getMedida()).isEqualTo(MedidaModeracao.SUSPENSAO);
            assertThat(sancao.getExpiraEm()).isAfter(antes.plusDays(6));

            assertThat(avaliador.getSuspensoAte()).isEqualTo(sancao.getExpiraEm());
            assertThat(avaliador.estaSuspenso()).isTrue();
            verify(usuarioRepository).save(avaliador);
        }

        @Test
        void suspensaoNovaNaoDeveEncurtarUmaSuspensaoMaisLongaJaVigente() {
            LocalDateTime prazoLongo = LocalDateTime.now().plusDays(30);
            avaliador.setSuspensoAte(prazoLongo);

            Denuncia denuncia = EntityFixtures.denunciaDePerfil(avaliador, terceiro, "Outra denúncia");
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));
            when(usuarioRepository.getReferenceById(admin.getId())).thenReturn(admin);

            denunciaService.resolver(
                    denuncia.getId(),
                    pedidoDeResolucao(MedidaModeracao.SUSPENSAO, 1, "Mais uma ocorrência."),
                    admin.getId());

            assertThat(avaliador.getSuspensoAte()).isEqualTo(prazoLongo);
        }

        @Test
        void deveExigirJustificativaQuandoAMedidaPuneAlguem() {
            Denuncia denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Comentário abusivo");
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));

            assertThatThrownBy(() -> denunciaService.resolver(
                    denuncia.getId(), pedidoDeResolucao(MedidaModeracao.ADVERTENCIA, null, "   "), admin.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("justificativa");

            verify(sancaoRepository, never()).save(any());
            assertThat(avaliacao.getRemovida()).isFalse();
        }

        @Test
        void deveExigirPrazoQuandoAMedidaForSuspensao() {
            Denuncia denuncia = EntityFixtures.denunciaDePerfil(avaliador, terceiro, "Perfil abusivo");
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));

            assertThatThrownBy(() -> denunciaService.resolver(
                    denuncia.getId(),
                    pedidoDeResolucao(MedidaModeracao.SUSPENSAO, null, "Conteúdo grave."),
                    admin.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Informe por quantos dias a suspensão vale.");

            verify(sancaoRepository, never()).save(any());
        }

        @Test
        void naoDevePermitirRemoverConteudoDeUmPerfil() {
            Denuncia denuncia = EntityFixtures.denunciaDePerfil(avaliador, terceiro, "Perfil abusivo");
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));

            assertThatThrownBy(() -> denunciaService.resolver(
                    denuncia.getId(),
                    pedidoDeResolucao(MedidaModeracao.REMOVER_CONTEUDO, null, null),
                    admin.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("não é conteúdo removível");

            verify(sancaoRepository, never()).save(any());
        }

        @Test
        void suspenderCriadorDeProjetoDeveTambemInativarOProjetoDenunciado() {
            Denuncia denuncia = EntityFixtures.denunciaDeProjeto(projeto, terceiro, "Projeto é golpe");
            when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));
            when(usuarioRepository.getReferenceById(admin.getId())).thenReturn(admin);

            denunciaService.resolver(
                    denuncia.getId(),
                    pedidoDeResolucao(MedidaModeracao.SUSPENSAO, 30, "Projeto usado para aplicar golpe."),
                    admin.getId());

            verify(projetoService).inativarPorModeracao(projeto.getId());
            verify(sancaoRepository).save(any(Sancao.class));
            assertThat(avaliador.estaSuspenso()).isTrue();
        }
    }
}
