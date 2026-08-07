package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Avaliacao;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.avaliacao.AvaliacaoResponse;
import com.unifapi.academico.dto.avaliacao.AvaliarRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.AvaliacaoRepository;
import com.unifapi.academico.repository.ProjetoMembroRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Avaliação entre pessoas que participaram do mesmo projeto (criador ou membro).
 * Só é permitida depois que o projeto chega a um estado final (CONCLUIDO ou
 * CANCELADO), pra não virar ferramenta de retaliação no meio da colaboração.
 */
@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final ProjetoRepository projetoRepository;
    private final ProjetoMembroRepository projetoMembroRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public AvaliacaoResponse avaliar(UUID projetoId, AvaliarRequest request, UUID avaliadorId) {
        if (avaliadorId.equals(request.avaliadoId())) {
            throw new RegraDeNegocioException("Você não pode avaliar a si mesmo.");
        }

        // findById direto (não findByIdAndAtivoTrue): a avaliação é sobre uma colaboração
        // passada, que continua avaliável mesmo se o criador inativar o projeto depois.
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado."));

        if (projeto.getStatus() != StatusProjeto.CONCLUIDO && projeto.getStatus() != StatusProjeto.CANCELADO) {
            throw new RegraDeNegocioException(
                    "Só é possível avaliar participantes depois que o projeto é concluído ou cancelado.");
        }

        if (!participouDoProjeto(projeto, avaliadorId)) {
            throw new RegraDeNegocioException("Você não participou deste projeto.");
        }

        Usuario avaliado = usuarioRepository.findById(request.avaliadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário avaliado não encontrado."));

        if (!participouDoProjeto(projeto, avaliado.getId())) {
            throw new RegraDeNegocioException("Esta pessoa não participou deste projeto.");
        }

        if (avaliacaoRepository.existsByProjeto_IdAndAvaliador_IdAndAvaliado_Id(
                projetoId, avaliadorId, avaliado.getId())) {
            throw new RegraDeNegocioException("Você já avaliou esta pessoa neste projeto.");
        }

        Usuario avaliador = usuarioRepository.getReferenceById(avaliadorId);

        Avaliacao avaliacao = avaliacaoRepository.save(Avaliacao.builder()
                .projeto(projeto)
                .avaliador(avaliador)
                .avaliado(avaliado)
                .nota(request.nota())
                .comentario(normalizar(request.comentario()))
                .build());

        avaliacaoRepository.flush();
        recalcularMedia(avaliado.getId());

        return AvaliacaoResponse.fromEntity(avaliacao);
    }

    @Transactional(readOnly = true)
    public Page<AvaliacaoResponse> listarPorAvaliado(UUID avaliadoId, Pageable pageable) {
        return avaliacaoRepository.findByAvaliado_IdOrderByCriadoEmDesc(avaliadoId, pageable)
                .map(AvaliacaoResponse::fromEntity);
    }

    private boolean participouDoProjeto(Projeto projeto, UUID usuarioId) {
        boolean ehCriador = projeto.getCriador() != null && projeto.getCriador().getId().equals(usuarioId);
        return ehCriador || projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), usuarioId);
    }

    private void recalcularMedia(UUID usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            usuario.setNotaMedia(avaliacaoRepository.calcularMediaPorAvaliado(usuarioId));
            usuario.setTotalAvaliacoes((int) avaliacaoRepository.countByAvaliado_Id(usuarioId));
            usuarioRepository.save(usuario);
        });
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
