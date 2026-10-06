package com.unifapi.academico.dto.avaliacao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AvaliarRequest(

        @NotNull(message = "ID do usuário avaliado é obrigatório")
        UUID avaliadoId,

        @NotNull(message = "Nota é obrigatória")
        @Min(value = 1, message = "Nota mínima é 1")
        @Max(value = 5, message = "Nota máxima é 5")
        Integer nota,

        @Size(max = 1000, message = "Comentário deve ter no máximo 1000 caracteres")
        String comentario
) {
}
