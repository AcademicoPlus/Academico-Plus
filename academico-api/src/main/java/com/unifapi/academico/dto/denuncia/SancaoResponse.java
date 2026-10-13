package com.unifapi.academico.dto.denuncia;

import com.unifapi.academico.domain.enums.MedidaModeracao;
import com.unifapi.academico.domain.models.Sancao;

import java.time.LocalDateTime;
import java.util.UUID;

/** Advertência ou suspensão, como o usuário sancionado a vê (no login). */
public record SancaoResponse(
        UUID id,
        MedidaModeracao medida,
        String justificativa,
        LocalDateTime criadoEm,
        /** Fim da suspensão; nulo em advertência. */
        LocalDateTime expiraEm
) {

    public static SancaoResponse fromEntity(Sancao sancao) {
        return new SancaoResponse(
                sancao.getId(),
                sancao.getMedida(),
                sancao.getJustificativa(),
                sancao.getCriadoEm(),
                sancao.getExpiraEm()
        );
    }
}
