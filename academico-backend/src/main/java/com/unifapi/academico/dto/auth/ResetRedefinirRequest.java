package com.unifapi.academico.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ResetRedefinirRequest(
        @NotBlank(message = "O token de redefinição é obrigatório.")
        String tokenRedefinicao,

        @NotBlank(message = "A nova senha é obrigatória.")
        String novaSenha
) {}
