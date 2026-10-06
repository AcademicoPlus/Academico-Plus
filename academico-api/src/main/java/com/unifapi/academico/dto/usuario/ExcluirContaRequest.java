package com.unifapi.academico.dto.usuario;

import jakarta.validation.constraints.NotBlank;

/** Exige a senha atual para evitar que uma sessão sequestrada apague a conta sem o dono perceber. */
public record ExcluirContaRequest(
        @NotBlank(message = "Senha é obrigatória para confirmar a exclusão da conta")
        String senha
) {
}
