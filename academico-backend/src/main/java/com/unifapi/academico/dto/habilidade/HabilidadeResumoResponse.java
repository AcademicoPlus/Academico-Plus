package com.unifapi.academico.dto.habilidade;

import com.unifapi.academico.domain.models.Habilidade;

import java.util.UUID;

/** Referência mínima a uma habilidade, usada em listas aninhadas. */
public record HabilidadeResumoResponse(
        UUID id,
        String nome,
        String categoria
) {
    public static HabilidadeResumoResponse fromEntity(Habilidade habilidade) {
        if (habilidade == null) {
            return null;
        }
        return new HabilidadeResumoResponse(
                habilidade.getId(),
                habilidade.getNome(),
                habilidade.getCategoria()
        );
    }
}
