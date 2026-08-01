package com.unifapi.academico.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL) // Oculta o campo 'erros' quando for nulo
public record ErroResposta(
    int status,
    String erro,
    String mensagem,
    String path,
    LocalDateTime timestamp,
    List<ErroCampo> erros
) {
    // Factory method para erros simples
    public static ErroResposta simples(int status, String erro, String mensagem, String path) {
        return new ErroResposta(status, erro, mensagem, path, LocalDateTime.now(), null);
    }

    // Factory method para erros com lista de validação
    public static ErroResposta comErros(int status, String erro, String mensagem, String path, List<ErroCampo> erros) {
        return new ErroResposta(status, erro, mensagem, path, LocalDateTime.now(), erros);
    }
}