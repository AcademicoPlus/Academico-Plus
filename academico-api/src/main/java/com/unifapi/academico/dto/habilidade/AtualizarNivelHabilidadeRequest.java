package com.unifapi.academico.dto.habilidade;

import com.unifapi.academico.domain.enums.NivelHabilidade;
import jakarta.validation.constraints.NotNull;

public record AtualizarNivelHabilidadeRequest(

        @NotNull(message = "Nível é obrigatório")
        NivelHabilidade nivel
) {
}
