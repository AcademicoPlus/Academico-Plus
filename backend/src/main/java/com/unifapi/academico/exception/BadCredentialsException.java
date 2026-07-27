package com.unifapi.academico.exception;

public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String mensagem) {
        super(mensagem);
    }
}