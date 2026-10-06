package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.models.Avaliacao;
import com.unifapi.academico.domain.models.DenunciaAvaliacao;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.avaliacao.DenunciaResponse;
import com.unifapi.academico.dto.avaliacao.DenunciarAvaliacaoRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.AvaliacaoRepository;
import com.unifapi.academico.repository.DenunciaAvaliacaoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.support.EntityFixtures;
import org.junit.jupiter.api.BeforeEach;
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
 * (sem contexto Spring). Cobre as regras de negócio de denúncia e resolução
 * descritas na Javadoc da classe: só o avaliado pode denunciar, não pode
 * denunciar duas vezes, e uma denúncia só pode ser resolvida uma única vez.
 */
@ExtendWith(MockitoExtension.class)
class DenunciaServiceTest {

    @Mock
    private DenunciaAvaliacaoRepository denunciaRepository;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AvaliacaoService avaliacaoService;

    @InjectMocks
    private DenunciaService denunciaService;

    private NivelAcesso nivelUsuario;
    private NivelAcesso nivelAdmin;
    private Usuario avaliador;
    private Usuario avaliado;
    private Usuario terceiro;
    private Usuario admin;
    private Projeto projeto;
    private Avaliacao avaliacao;

    @BeforeEach
    void setUp() {
        nivelUsuario = EntityFixtures.nivelAcesso("USER");
        nivelAdmin = EntityFixtures.nivelAcesso("ADMIN");

        avaliador = EntityFixtures.usuario("avaliador@teste.com", nivelUsuario);
        avaliado = EntityFixtures.usuario("avaliado@teste.com", nivelUsuario);
        terceiro = EntityFixtures.usuario("terceiro@teste.com", nivelUsuario);
        admin = EntityFixtures.usuario("admin@teste.com", nivelAdmin);

        projeto = EntityFixtures.projeto(avaliador, 3);
        avaliacao = EntityFixtures.avaliacao(projeto, avaliador, avaliado, 2);
    }

    // ---------------------------------------------------------------
    // denunciar
    // ---------------------------------------------------------------

    @Test
    void deveLancarExcecaoQuandoAvaliacaoNaoEncontrada() {
        UUID avaliacaoId = UUID.randomUUID();
        when(avaliacaoRepository.findById(avaliacaoId)).thenReturn(Optional.empty());

        DenunciarAvaliacaoRequest request = new DenunciarAvaliacaoRequest("Motivo qualquer com mais de dez chars");

        assertThatThrownBy(() -> denunciaService.denunciar(avaliacaoId, request, avaliado.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Avaliação não encontrada.");

        verifyNoInteractions(denunciaRepository);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveLancarExcecaoQuandoAvaliacaoNaoTemAvaliadoDefinido() {
        Avaliacao avaliacaoSemAvaliado = Avaliacao.builder()
                .id(UUID.randomUUID())
                .projeto(projeto)
                .avaliador(avaliador)
                .avaliado(null)
                .nota(3)
                .removida(false)
                .criadoEm(LocalDateTime.now())
                .build();
        when(avaliacaoRepository.findById(avaliacaoSemAvaliado.getId())).thenReturn(Optional.of(avaliacaoSemAvaliado));

        DenunciarAvaliacaoRequest request = new DenunciarAvaliacaoRequest("Motivo qualquer com mais de dez chars");

        assertThatThrownBy(() -> denunciaService.denunciar(avaliacaoSemAvaliado.getId(), request, avaliado.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Você só pode denunciar avaliações recebidas por você.");

        verifyNoInteractions(denunciaRepository);
    }

    @Test
    void deveLancarExcecaoQuandoAvaliadorTentaDenunciarSuaPropriaAvaliacaoFeita() {
        when(avaliacaoRepository.findById(avaliacao.getId())).thenReturn(Optional.of(avaliacao));

        DenunciarAvaliacaoRequest request = new DenunciarAvaliacaoRequest("Motivo qualquer com mais de dez chars");

        // O avaliador (quem AVALIOU) tenta denunciar a própria avaliação que fez - não permitido,
        // só quem foi avaliado pode denunciar.
        assertThatThrownBy(() -> denunciaService.denunciar(avaliacao.getId(), request, avaliador.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Você só pode denunciar avaliações recebidas por você.");

        verifyNoInteractions(denunciaRepository);
    }

    @Test
    void deveLancarExcecaoQuandoDenuncianteForTerceiroNaoRelacionadoAAvaliacao() {
        when(avaliacaoRepository.findById(avaliacao.getId())).thenReturn(Optional.of(avaliacao));

        DenunciarAvaliacaoRequest request = new DenunciarAvaliacaoRequest("Motivo qualquer com mais de dez chars");

        assertThatThrownBy(() -> denunciaService.denunciar(avaliacao.getId(), request, terceiro.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Você só pode denunciar avaliações recebidas por você.");

        verifyNoInteractions(denunciaRepository);
    }

    @Test
    void deveLancarExcecaoQuandoJaExisteDenunciaDoMesmoDenuncianteParaMesmaAvaliacao() {
        when(avaliacaoRepository.findById(avaliacao.getId())).thenReturn(Optional.of(avaliacao));
        when(denunciaRepository.existsByAvaliacao_IdAndDenunciante_Id(avaliacao.getId(), avaliado.getId()))
                .thenReturn(true);

        DenunciarAvaliacaoRequest request = new DenunciarAvaliacaoRequest("Motivo qualquer com mais de dez chars");

        assertThatThrownBy(() -> denunciaService.denunciar(avaliacao.getId(), request, avaliado.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Você já denunciou esta avaliação.");

        // Short-circuita antes de buscar a referência do usuário e salvar.
        verifyNoInteractions(usuarioRepository);
        verify(denunciaRepository, never()).save(any());
    }

    @Test
    void devePermitirQuePessoaAvaliadaDenuncieAAvaliacaoRecebida() {
        when(avaliacaoRepository.findById(avaliacao.getId())).thenReturn(Optional.of(avaliacao));
        when(denunciaRepository.existsByAvaliacao_IdAndDenunciante_Id(avaliacao.getId(), avaliado.getId()))
                .thenReturn(false);
        when(usuarioRepository.getReferenceById(avaliado.getId())).thenReturn(avaliado);
        when(denunciaRepository.save(any(DenunciaAvaliacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DenunciarAvaliacaoRequest request =
                new DenunciarAvaliacaoRequest("   Comentário ofensivo e sem relação com o projeto.   ");

        DenunciaResponse response = denunciaService.denunciar(avaliacao.getId(), request, avaliado.getId());

        ArgumentCaptor<DenunciaAvaliacao> captor = ArgumentCaptor.forClass(DenunciaAvaliacao.class);
        verify(denunciaRepository).save(captor.capture());

        DenunciaAvaliacao salva = captor.getValue();
        assertThat(salva.getStatus()).isEqualTo(StatusDenuncia.PENDENTE);
        assertThat(salva.getMotivo()).isEqualTo("Comentário ofensivo e sem relação com o projeto.");
        assertThat(salva.getAvaliacao()).isEqualTo(avaliacao);
        assertThat(salva.getDenunciante()).isEqualTo(avaliado);

        assertThat(response.status()).isEqualTo(StatusDenuncia.PENDENTE);
        assertThat(response.motivo()).isEqualTo("Comentário ofensivo e sem relação com o projeto.");
        assertThat(response.avaliacaoId()).isEqualTo(avaliacao.getId());
    }

    @Test
    void devePermitirDenunciaDeAvaliacaoQueJaEstaMarcadaComoRemovida() {
        // Não há guarda no serviço contra denunciar uma avaliação já removida
        // (removida=true) — o fluxo de denúncia continua funcionando normalmente.
        avaliacao.setRemovida(true);

        when(avaliacaoRepository.findById(avaliacao.getId())).thenReturn(Optional.of(avaliacao));
        when(denunciaRepository.existsByAvaliacao_IdAndDenunciante_Id(avaliacao.getId(), avaliado.getId()))
                .thenReturn(false);
        when(usuarioRepository.getReferenceById(avaliado.getId())).thenReturn(avaliado);
        when(denunciaRepository.save(any(DenunciaAvaliacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DenunciarAvaliacaoRequest request = new DenunciarAvaliacaoRequest("Motivo qualquer com mais de dez chars");

        DenunciaResponse response = denunciaService.denunciar(avaliacao.getId(), request, avaliado.getId());

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StatusDenuncia.PENDENTE);
        verify(denunciaRepository).save(any(DenunciaAvaliacao.class));
    }

    // ---------------------------------------------------------------
    // listarPendentes
    // ---------------------------------------------------------------

    @Test
    void deveListarDenunciasPendentesOrdenadasPorCriadoEmAscendente() {
        Pageable pageable = PageRequest.of(0, 10);
        DenunciaAvaliacao denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Motivo registrado na denúncia");
        Page<DenunciaAvaliacao> page = new PageImpl<>(List.of(denuncia), pageable, 1);

        when(denunciaRepository.findByStatusOrderByCriadoEmAsc(StatusDenuncia.PENDENTE, pageable)).thenReturn(page);

        Page<DenunciaResponse> resultado = denunciaService.listarPendentes(pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).motivo()).isEqualTo("Motivo registrado na denúncia");
        assertThat(resultado.getContent().get(0).status()).isEqualTo(StatusDenuncia.PENDENTE);

        verify(denunciaRepository).findByStatusOrderByCriadoEmAsc(eq(StatusDenuncia.PENDENTE), eq(pageable));
    }

    // ---------------------------------------------------------------
    // resolver
    // ---------------------------------------------------------------

    @Test
    void deveLancarExcecaoQuandoDenunciaNaoEncontrada() {
        UUID denunciaId = UUID.randomUUID();
        when(denunciaRepository.findById(denunciaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> denunciaService.resolver(denunciaId, true, admin.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Denúncia não encontrada.");

        verifyNoInteractions(avaliacaoRepository);
        verifyNoInteractions(avaliacaoService);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveLancarExcecaoQuandoDenunciaJaFoiAnalisadaComoProcedente() {
        DenunciaAvaliacao denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Motivo já analisado");
        denuncia.setStatus(StatusDenuncia.PROCEDENTE);
        when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));

        // Mesmo tentando "corrigir" uma decisão anterior, uma denúncia só pode ser resolvida uma vez.
        assertThatThrownBy(() -> denunciaService.resolver(denuncia.getId(), false, admin.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Esta denúncia já foi analisada.");

        verifyNoInteractions(avaliacaoRepository);
        verifyNoInteractions(avaliacaoService);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveLancarExcecaoQuandoDenunciaJaFoiAnalisadaComoImprocedente() {
        DenunciaAvaliacao denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Motivo já analisado");
        denuncia.setStatus(StatusDenuncia.IMPROCEDENTE);
        when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));

        assertThatThrownBy(() -> denunciaService.resolver(denuncia.getId(), true, admin.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Esta denúncia já foi analisada.");

        verifyNoInteractions(avaliacaoRepository);
        verifyNoInteractions(avaliacaoService);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveMarcarDenunciaComoProcedenteRemoverAvaliacaoERecalcularMediaDoAvaliado() {
        DenunciaAvaliacao denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Motivo procedente");
        when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));
        when(usuarioRepository.getReferenceById(admin.getId())).thenReturn(admin);

        LocalDateTime antes = LocalDateTime.now().minusSeconds(2);

        DenunciaResponse response = denunciaService.resolver(denuncia.getId(), true, admin.getId());

        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.PROCEDENTE);
        assertThat(denuncia.getAnalisadoPor()).isEqualTo(admin);
        assertThat(denuncia.getAnalisadoEm()).isNotNull().isAfter(antes);
        assertThat(response.status()).isEqualTo(StatusDenuncia.PROCEDENTE);

        assertThat(avaliacao.getRemovida()).isTrue();

        verify(avaliacaoRepository).save(avaliacao);
        verify(avaliacaoRepository).flush();
        verify(avaliacaoService, times(1)).recalcularMedia(avaliado.getId());
    }

    @Test
    void deveMarcarDenunciaComoImprocedenteSemAlterarAAvaliacao() {
        DenunciaAvaliacao denuncia = EntityFixtures.denuncia(avaliacao, avaliado, "Motivo improcedente");
        when(denunciaRepository.findById(denuncia.getId())).thenReturn(Optional.of(denuncia));
        when(usuarioRepository.getReferenceById(admin.getId())).thenReturn(admin);

        LocalDateTime antes = LocalDateTime.now().minusSeconds(2);

        DenunciaResponse response = denunciaService.resolver(denuncia.getId(), false, admin.getId());

        assertThat(denuncia.getStatus()).isEqualTo(StatusDenuncia.IMPROCEDENTE);
        assertThat(denuncia.getAnalisadoPor()).isEqualTo(admin);
        assertThat(denuncia.getAnalisadoEm()).isNotNull().isAfter(antes);
        assertThat(response.status()).isEqualTo(StatusDenuncia.IMPROCEDENTE);

        // A avaliação denunciada permanece intocada quando a denúncia é julgada improcedente.
        assertThat(avaliacao.getRemovida()).isFalse();

        verify(avaliacaoRepository, never()).save(any());
        verify(avaliacaoRepository, never()).flush();
        verify(avaliacaoService, never()).recalcularMedia(any());
    }
}
