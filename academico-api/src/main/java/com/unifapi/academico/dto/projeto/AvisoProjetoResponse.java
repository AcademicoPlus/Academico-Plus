package com.unifapi.academico.dto.projeto;

import com.unifapi.academico.domain.models.ProjetoCanceladoAviso;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aviso pendente sobre um projeto de que o usuário é membro.
 *
 * @param tipo   "cancelamento" ou "conclusao"
 * @param motivo motivo informado pelo criador ao cancelar; nulo nos demais casos
 */
public record AvisoProjetoResponse(
        UUID id,
        UUID projetoId,
        String projetoNome,
        String tipo,
        String motivo,
        LocalDateTime criadoEm
) {
    public static AvisoProjetoResponse fromEntity(ProjetoCanceladoAviso aviso) {
        return new AvisoProjetoResponse(
                aviso.getId(),
                aviso.getProjetoId(),
                aviso.getProjetoNome(),
                aviso.getTipo(),
                aviso.getMotivo(),
                aviso.getCriadoEm()
        );
    }
}
