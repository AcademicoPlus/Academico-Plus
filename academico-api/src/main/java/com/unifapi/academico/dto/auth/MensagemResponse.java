package com.unifapi.academico.dto.auth;

public record MensagemResponse(
        boolean sucesso,
        String mensagem
) {}