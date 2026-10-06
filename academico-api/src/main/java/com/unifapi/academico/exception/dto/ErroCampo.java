package com.unifapi.academico.exception.dto;

public record ErroCampo(
    String campo,
    String mensagem
) {}