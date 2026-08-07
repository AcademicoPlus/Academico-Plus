package com.unifapi.academico.dto.auth;

public record CadastroResponse(
        boolean sucesso,
        String mensagem
) {}