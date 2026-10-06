package com.unifapi.academico.dto.habilidade;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Vincula uma habilidade já existente do catálogo a um projeto. */
public record VincularHabilidadeRequest(

        @NotNull(message = "ID da habilidade é obrigatório")
        UUID habilidadeId,

        /** Quando nulo, assume false (habilidade desejável, não obrigatória). */
        Boolean obrigatoria
) {
}
