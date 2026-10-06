package com.unifapi.academico.dto.habilidade;

import com.unifapi.academico.domain.enums.NivelHabilidade;
import com.unifapi.academico.domain.models.UsuarioHabilidade;

import java.util.UUID;

public record UsuarioHabilidadeResponse(
        UUID id,
        HabilidadeResumoResponse habilidade,
        NivelHabilidade nivel
) {
    public static UsuarioHabilidadeResponse fromEntity(UsuarioHabilidade vinculo) {
        return new UsuarioHabilidadeResponse(
                vinculo.getId(),
                HabilidadeResumoResponse.fromEntity(vinculo.getHabilidade()),
                vinculo.getNivel()
        );
    }
}
