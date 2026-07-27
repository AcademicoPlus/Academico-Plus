package com.unifapi.academico.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String mensagem) {
        super(mensagem);
    }
}