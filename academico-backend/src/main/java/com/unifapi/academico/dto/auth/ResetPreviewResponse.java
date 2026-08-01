package com.unifapi.academico.dto.auth;

public record ResetPreviewResponse(
        boolean sucesso,
        String mensagem,
        String email,
        String emailPreview
) {}