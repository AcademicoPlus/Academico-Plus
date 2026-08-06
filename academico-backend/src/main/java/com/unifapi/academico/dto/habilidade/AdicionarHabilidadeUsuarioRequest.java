package com.unifapi.academico.dto.habilidade;

import com.unifapi.academico.domain.enums.NivelHabilidade;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdicionarHabilidadeUsuarioRequest(

        @NotNull(message = "ID da habilidade é obrigatório")
        UUID habilidadeId,

        /** Quando nulo, o service assume NivelHabilidade.INICIANTE. */
        NivelHabilidade nivel
) {
}
