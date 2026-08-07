package com.unifapi.academico.dto.candidatura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejeitarCandidaturaRequest(

        @NotBlank(message = "Motivo da rejeição é obrigatório")
        @Size(min = 5, max = 500, message = "Motivo deve ter entre 5 e 500 caracteres")
        String motivoRejeicao
) {
}
