package com.unifapi.academico.dto.avaliacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DenunciarAvaliacaoRequest(

        @NotBlank(message = "O motivo da denúncia é obrigatório")
        @Size(min = 10, max = 1000, message = "Motivo deve ter entre 10 e 1000 caracteres")
        String motivo
) {
}
