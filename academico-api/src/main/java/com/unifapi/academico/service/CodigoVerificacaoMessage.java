package com.unifapi.academico.service;

/** Contrato publicado na fila de e-mail — precisa bater com o record equivalente no academico-email-service. */
public record CodigoVerificacaoMessage(String destinatario, String codigo, int minutosExpiracao) {
}
