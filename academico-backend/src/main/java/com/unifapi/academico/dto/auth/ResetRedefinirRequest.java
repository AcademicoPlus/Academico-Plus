package com.unifapi.academico.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetRedefinirRequest(
        @NotBlank(message = "O token de redefinição é obrigatório.")
        String tokenRedefinicao,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres")
        String novaSenha
) {}
