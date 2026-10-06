package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.models.Avaliacao;
import com.unifapi.academico.domain.models.DenunciaAvaliacao;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.avaliacao.DenunciaResponse;
import com.unifapi.academico.dto.avaliacao.DenunciarAvaliacaoRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.AvaliacaoRepository;
import com.unifapi.academico.repository.DenunciaAvaliacaoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Denúncia de uma avaliação recebida, feita pela própria pessoa avaliada.
 * Analisada por um ADMIN, que decide se a avaliação é removida (soft-delete,
 * ver Avaliacao.removida) ou se a denúncia é arquivada como improcedente.
 */
@Service
@RequiredArgsConstructor
public class DenunciaService {

    private final DenunciaAvaliacaoRepository denunciaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoService avaliacaoService;

    @Transactional
    public DenunciaResponse denunciar(UUID avaliacaoId, DenunciarAvaliacaoRequest request, UUID denuncianteId) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada."));

        if (avaliacao.getAvaliado() == null || !avaliacao.getAvaliado().getId().equals(denuncianteId)) {
            throw new RegraDeNegocioException("Você só pode denunciar avaliações recebidas por você.");
        }

        if (denunciaRepository.existsByAvaliacao_IdAndDenunciante_Id(avaliacaoId, denuncianteId)) {
            throw new RegraDeNegocioException("Você já denunciou esta avaliação.");
        }

        Usuario denunciante = usuarioRepository.getReferenceById(denuncianteId);

        DenunciaAvaliacao denuncia = denunciaRepository.save(DenunciaAvaliacao.builder()
                .avaliacao(avaliacao)
                .denunciante(denunciante)
                .motivo(request.motivo().trim())
                .status(StatusDenuncia.PENDENTE)
                .build());

        return DenunciaResponse.fromEntity(denuncia);
    }

    @Transactional(readOnly = true)
    public Page<DenunciaResponse> listarPendentes(Pageable pageable) {
        return denunciaRepository.findByStatusOrderByCriadoEmAsc(StatusDenuncia.PENDENTE, pageable)
                .map(DenunciaResponse::fromEntity);
    }

    /**
     * Se procedente, marca a avaliação como removida (some da listagem pública e
     * some do cálculo da média) e recalcula a nota do avaliado. Se improcedente,
     * só arquiva a denúncia — a avaliação permanece como estava.
     */
    @Transactional
    public DenunciaResponse resolver(UUID denunciaId, boolean procedente, UUID adminId) {
        DenunciaAvaliacao denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denúncia não encontrada."));

        if (denuncia.getStatus() != StatusDenuncia.PENDENTE) {
            throw new RegraDeNegocioException("Esta denúncia já foi analisada.");
        }

        denuncia.setStatus(procedente ? StatusDenuncia.PROCEDENTE : StatusDenuncia.IMPROCEDENTE);
        denuncia.setAnalisadoPor(usuarioRepository.getReferenceById(adminId));
        denuncia.setAnalisadoEm(LocalDateTime.now());

        if (procedente) {
            Avaliacao avaliacao = denuncia.getAvaliacao();
            avaliacao.setRemovida(true);
            avaliacaoRepository.save(avaliacao);
            avaliacaoRepository.flush();
            avaliacaoService.recalcularMedia(avaliacao.getAvaliado().getId());
        }

        return DenunciaResponse.fromEntity(denuncia);
    }
}
