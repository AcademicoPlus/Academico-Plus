package com.unifapi.academico.dto.auth;

public record ResetValidarResponse(
        boolean sucesso,
        String tokenRedefinicao
) {}