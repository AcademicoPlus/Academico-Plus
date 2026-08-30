package com.unifapi.academico.email.messaging;

public record CodigoVerificacaoMessage(String destinatario, String codigo, int minutosExpiracao) {
}
