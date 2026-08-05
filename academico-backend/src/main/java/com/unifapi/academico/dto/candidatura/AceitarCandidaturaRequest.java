package com.unifapi.academico.dto.candidatura;

import jakarta.validation.constraints.Size;

/**
 * Corpo opcional ao aceitar uma candidatura: permite já definir a função
 * do novo membro no projeto.
 */
public record AceitarCandidaturaRequest(

        @Size(max = 100, message = "Função deve ter no máximo 100 caracteres")
        String funcao
) {
}
