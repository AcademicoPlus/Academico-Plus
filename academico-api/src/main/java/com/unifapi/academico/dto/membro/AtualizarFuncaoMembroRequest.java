package com.unifapi.academico.dto.membro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarFuncaoMembroRequest(

        @NotBlank(message = "Função é obrigatória")
        @Size(max = 100, message = "Função deve ter no máximo 100 caracteres")
        String funcao
) {
}
