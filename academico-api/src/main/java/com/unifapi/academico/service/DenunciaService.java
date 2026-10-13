package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.MedidaModeracao;
import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.enums.TipoAlvoDenuncia;
import com.unifapi.academico.domain.models.Avaliacao;
import com.unifapi.academico.domain.models.Denuncia;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Sancao;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.denuncia.DenunciaResponse;
import com.unifapi.academico.dto.denuncia.DenunciarRequest;
import com.unifapi.academico.dto.denuncia.HistoricoModeracaoResponse;
import com.unifapi.academico.dto.denuncia.ResolverDenunciaRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.AvaliacaoRepository;
import com.unifapi.academico.repository.DenunciaRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.SancaoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Denúncias de avaliação, projeto e perfil, e a escada de sanções aplicada
 * pelo ADMIN ao julgá-las.
 *
 * A escada é "primeiro advertir, depois suspender", mas quem escolhe a medida
 * é sempre o admin: {@link #sugerirMedida} apenas recomenda a partir do
 * histórico do responsável, e a fila entrega essa sugestão junto da denúncia.
 * Automatizar a subida trataria um spam leve igual a um golpe, e numa
 * plataforma com poucos moderadores o erro sairia caro demais para reverter.
 */
@Service
@RequiredArgsConstructor
public class DenunciaService {

    /** A partir daqui a sugestão sobe de advertência para suspensão. */
    private static final long ADVERTENCIAS_ATE_SUGERIR_SUSPENSAO = 2;

    /** Prazo proposto na UI quando a sugestão é suspender; o admin pode mudar. */
    private static final int DIAS_SUSPENSAO_SUGERIDOS = 7;

    private final DenunciaRepository denunciaRepository;
    private final SancaoRepository sancaoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoService avaliacaoService;
    private final ProjetoService projetoService;

    // ============================================
    // CRIAÇÃO DE DENÚNCIA (um método por tipo de alvo)
    // ============================================

    /**
     * Avaliação só pode ser denunciada por quem a recebeu — regra que já valia
     * antes de a denúncia alcançar projetos e perfis, e que permanece mais
     * fechada que as outras de propósito: a avaliação é dirigida a uma pessoa
     * específica, então só ela tem contexto para dizer que é abusiva.
     */
    @Transactional
    public DenunciaResponse denunciarAvaliacao(UUID avaliacaoId, DenunciarRequest request, UUID denuncianteId) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada."));

        if (avaliacao.getAvaliado() == null || !avaliacao.getAvaliado().getId().equals(denuncianteId)) {
            throw new RegraDeNegocioException("Você só pode denunciar avaliações recebidas por você.");
        }

        if (denunciaRepository.existsByAvaliacao_IdAndDenunciante_Id(avaliacaoId, denuncianteId)) {
            throw new RegraDeNegocioException("Você já denunciou esta avaliação.");
        }

        return salvar(Denuncia.builder()
                .tipoAlvo(TipoAlvoDenuncia.AVALIACAO)
                .avaliacao(avaliacao), request, denuncianteId);
    }

    /** Qualquer usuário autenticado pode denunciar um projeto que não seja seu. */
    @Transactional
    public DenunciaResponse denunciarProjeto(UUID projetoId, DenunciarRequest request, UUID denuncianteId) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado."));

        if (projeto.getCriador() != null && projeto.getCriador().getId().equals(denuncianteId)) {
            throw new RegraDeNegocioException(
                    "Você não pode denunciar um projeto seu. Para encerrá-lo, use a opção de excluir projeto.");
        }

        if (denunciaRepository.existsByProjeto_IdAndDenunciante_Id(projetoId, denuncianteId)) {
            throw new RegraDeNegocioException("Você já denunciou este projeto.");
        }

        return salvar(Denuncia.builder()
                .tipoAlvo(TipoAlvoDenuncia.PROJETO)
                .projeto(projeto), request, denuncianteId);
    }

    /** Qualquer usuário autenticado pode denunciar o perfil de outra pessoa. */
    @Transactional
    public DenunciaResponse denunciarPerfil(UUID usuarioDenunciadoId, DenunciarRequest request, UUID denuncianteId) {
        if (usuarioDenunciadoId.equals(denuncianteId)) {
            throw new RegraDeNegocioException("Você não pode denunciar o próprio perfil.");
        }

        Usuario denunciado = usuarioRepository.findById(usuarioDenunciadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (denunciaRepository.existsByUsuarioDenunciado_IdAndDenunciante_Id(usuarioDenunciadoId, denuncianteId)) {
            throw new RegraDeNegocioException("Você já denunciou este perfil.");
        }

        return salvar(Denuncia.builder()
                .tipoAlvo(TipoAlvoDenuncia.PERFIL)
                .usuarioDenunciado(denunciado), request, denuncianteId);
    }

    private DenunciaResponse salvar(Denuncia.DenunciaBuilder builder, DenunciarRequest request, UUID denuncianteId) {
        Denuncia denuncia = denunciaRepository.save(builder
                .denunciante(usuarioRepository.getReferenceById(denuncianteId))
                .categoria(request.categoria())
                .motivo(request.motivo().trim())
                .status(StatusDenuncia.PENDENTE)
                .build());

        return DenunciaResponse.fromEntity(denuncia);
    }

    // ============================================
    // FILA DE REVISÃO (ADMIN)
    // ============================================

    /**
     * Fila de pendentes, opcionalmente filtrada por tipo de alvo. Cada item vem
     * com o histórico de moderação do responsável e a medida sugerida — sem
     * isso o admin não teria como aplicar a escada sem abrir cada perfil.
     */
    @Transactional(readOnly = true)
    public Page<DenunciaResponse> listarPendentes(TipoAlvoDenuncia tipoAlvo, Pageable pageable) {
        Page<Denuncia> pendentes = tipoAlvo == null
                ? denunciaRepository.findByStatusOrderByCriadoEmAsc(StatusDenuncia.PENDENTE, pageable)
                : denunciaRepository.findByStatusAndTipoAlvoOrderByCriadoEmAsc(
                        StatusDenuncia.PENDENTE, tipoAlvo, pageable);

        return pendentes.map(denuncia ->
                DenunciaResponse.fromEntity(denuncia, montarHistorico(denuncia.getResponsavel())));
    }

    /** Histórico de sanções do responsável + a medida que a escada recomenda. */
    private HistoricoModeracaoResponse montarHistorico(Usuario responsavel) {
        if (responsavel == null) {
            return null;
        }

        long advertencias = sancaoRepository.countByUsuario_IdAndMedida(
                responsavel.getId(), MedidaModeracao.ADVERTENCIA);
        long suspensoes = sancaoRepository.countByUsuario_IdAndMedida(
                responsavel.getId(), MedidaModeracao.SUSPENSAO);

        MedidaModeracao sugerida = sugerirMedida(advertencias);

        return new HistoricoModeracaoResponse(
                advertencias,
                suspensoes,
                responsavel.estaSuspenso(),
                responsavel.getSuspensoAte(),
                sugerida,
                justificarSugestao(advertencias, sugerida)
        );
    }

    /**
     * Primeira e segunda ocorrência: advertir. Da terceira em diante: suspender.
     * Só uma recomendação — nada impede o admin de suspender de cara um golpe,
     * nem de arquivar algo que não procede.
     */
    private MedidaModeracao sugerirMedida(long advertenciasAnteriores) {
        return advertenciasAnteriores >= ADVERTENCIAS_ATE_SUGERIR_SUSPENSAO
                ? MedidaModeracao.SUSPENSAO
                : MedidaModeracao.ADVERTENCIA;
    }

    private String justificarSugestao(long advertencias, MedidaModeracao sugerida) {
        if (sugerida == MedidaModeracao.SUSPENSAO) {
            return "Já acumula " + advertencias + " advertência(s): a escada recomenda suspender por "
                    + DIAS_SUSPENSAO_SUGERIDOS + " dias.";
        }
        return advertencias == 0
                ? "Sem histórico de sanções: a escada recomenda começar pela advertência."
                : "Tem " + advertencias + " advertência(s): ainda dentro da faixa de advertência.";
    }

    // ============================================
    // RESOLUÇÃO (ADMIN)
    // ============================================

    /**
     * Aplica a medida escolhida pelo admin e fecha a denúncia. As medidas são
     * cumulativas: advertir e suspender também derrubam o conteúdo denunciado.
     */
    @Transactional
    public DenunciaResponse resolver(UUID denunciaId, ResolverDenunciaRequest request, UUID adminId) {
        Denuncia denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denúncia não encontrada."));

        if (denuncia.getStatus() != StatusDenuncia.PENDENTE) {
            throw new RegraDeNegocioException("Esta denúncia já foi analisada.");
        }

        MedidaModeracao medida = request.medida();
        Usuario responsavel = denuncia.getResponsavel();

        validarMedida(denuncia, medida, responsavel, request);

        denuncia.setMedida(medida);
        denuncia.setStatus(medida.ehProcedente() ? StatusDenuncia.PROCEDENTE : StatusDenuncia.IMPROCEDENTE);
        denuncia.setAnalisadoPor(usuarioRepository.getReferenceById(adminId));
        denuncia.setAnalisadoEm(LocalDateTime.now());

        if (medida.removeConteudo()) {
            removerConteudo(denuncia);
        }

        if (medida.puneResponsavel()) {
            aplicarSancao(denuncia, responsavel, medida, request, adminId);
        }

        return DenunciaResponse.fromEntity(denuncia);
    }

    private void validarMedida(Denuncia denuncia, MedidaModeracao medida,
                               Usuario responsavel, ResolverDenunciaRequest request) {

        if (medida == MedidaModeracao.REMOVER_CONTEUDO && denuncia.getTipoAlvo() == TipoAlvoDenuncia.PERFIL) {
            throw new RegraDeNegocioException(
                    "Um perfil não é conteúdo removível: escolha advertir ou suspender o usuário.");
        }

        if (!medida.puneResponsavel()) {
            return;
        }

        if (responsavel == null) {
            throw new RegraDeNegocioException(
                    "Não há responsável identificável por este conteúdo para advertir ou suspender.");
        }

        if (request.justificativa() == null || request.justificativa().isBlank()) {
            throw new RegraDeNegocioException(
                    "Explique a decisão: a justificativa é o que o usuário sancionado vai ler.");
        }

        if (medida == MedidaModeracao.SUSPENSAO && request.diasSuspensao() == null) {
            throw new RegraDeNegocioException("Informe por quantos dias a suspensão vale.");
        }
    }

    /**
     * Avaliação sai por soft-delete (preserva o histórico para auditoria, RNF08)
     * e devolve a média do avaliado ao que seria sem ela; projeto é inativado
     * como se o criador o tivesse encerrado, avisando os membros.
     *
     * Perfil não tem conteúdo destacável para derrubar: advertir ou suspender
     * por causa de um perfil já é a medida inteira. Por isso aqui é um no-op, e
     * não um erro — a medida REMOVER_CONTEUDO sozinha sobre um perfil continua
     * barrada antes, em validarMedida, onde ela é de fato um engano do admin.
     */
    private void removerConteudo(Denuncia denuncia) {
        switch (denuncia.getTipoAlvo()) {
            case AVALIACAO -> {
                Avaliacao avaliacao = denuncia.getAvaliacao();
                avaliacao.setRemovida(true);
                avaliacaoRepository.save(avaliacao);
                avaliacaoRepository.flush();
                avaliacaoService.recalcularMedia(avaliacao.getAvaliado().getId());
            }
            case PROJETO -> projetoService.inativarPorModeracao(denuncia.getProjeto().getId());
            case PERFIL -> { /* nada a derrubar: a sanção ao usuário é a medida */ }
        }
    }

    private void aplicarSancao(Denuncia denuncia, Usuario responsavel, MedidaModeracao medida,
                               ResolverDenunciaRequest request, UUID adminId) {

        LocalDateTime expiraEm = medida == MedidaModeracao.SUSPENSAO
                ? LocalDateTime.now().plusDays(request.diasSuspensao())
                : null;

        sancaoRepository.save(Sancao.builder()
                .usuario(responsavel)
                .denuncia(denuncia)
                .medida(medida)
                .justificativa(request.justificativa().trim())
                .aplicadoPor(usuarioRepository.getReferenceById(adminId))
                .expiraEm(expiraEm)
                .build());

        if (expiraEm != null) {
            // Uma suspensão nova nunca encurta uma que já esteja correndo.
            LocalDateTime vigente = responsavel.getSuspensoAte();
            if (vigente == null || expiraEm.isAfter(vigente)) {
                responsavel.setSuspensoAte(expiraEm);
            }
            usuarioRepository.save(responsavel);
        }
    }
}
