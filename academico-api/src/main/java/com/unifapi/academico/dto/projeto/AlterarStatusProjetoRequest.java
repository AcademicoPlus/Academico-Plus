package com.unifapi.academico.dto.projeto;

import com.unifapi.academico.domain.enums.StatusProjeto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlterarStatusProjetoRequest(

        @NotNull(message = "Novo status é obrigatório")
        StatusProjeto status,

        /** Opcional, relevante ao cancelar: registrado no aviso enviado aos membros. */
        @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
        String motivo
) {
}
