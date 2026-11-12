package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.ProjetoCanceladoAviso;
import com.unifapi.academico.dto.projeto.AvisoProjetoResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.repository.ProjetoCanceladoAvisoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Avisos de projeto (cancelado, concluído) para os membros. Antes eles só eram
 * entregues — e marcados como vistos — na resposta do login, que o frontend
 * não exibia: o aviso se perdia. Agora o frontend consulta os pendentes durante
 * a sessão e só marca como visto quando a pessoa fecha o aviso.
 */
@Service
@RequiredArgsConstructor
public class AvisoProjetoService {

    private final ProjetoCanceladoAvisoRepository avisoRepository;

    @Transactional(readOnly = true)
    public List<AvisoProjetoResponse> listarPendentes(UUID usuarioId) {
        return avisoRepository.findByUsuario_IdAndVisualizadoEmIsNullOrderByCriadoEmAsc(usuarioId).stream()
                .map(AvisoProjetoResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void marcarComoVisualizado(UUID avisoId, UUID usuarioId) {
        ProjetoCanceladoAviso aviso = avisoRepository.findByIdAndUsuario_Id(avisoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aviso não encontrado."));

        if (aviso.getVisualizadoEm() == null) {
            aviso.setVisualizadoEm(LocalDateTime.now());
        }
    }
}
