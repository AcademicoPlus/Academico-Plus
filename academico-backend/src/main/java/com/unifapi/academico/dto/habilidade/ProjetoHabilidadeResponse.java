package com.unifapi.academico.dto.habilidade;

import com.unifapi.academico.domain.models.ProjetoHabilidade;

import java.util.UUID;

public record ProjetoHabilidadeResponse(
        UUID id,
        HabilidadeResumoResponse habilidade,
        Boolean obrigatoria
) {
    public static ProjetoHabilidadeResponse fromEntity(ProjetoHabilidade vinculo) {
        return new ProjetoHabilidadeResponse(
                vinculo.getId(),
                HabilidadeResumoResponse.fromEntity(vinculo.getHabilidade()),
                vinculo.getObrigatoria()
        );
    }
}
