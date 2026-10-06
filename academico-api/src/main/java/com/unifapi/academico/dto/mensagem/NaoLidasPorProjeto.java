package com.unifapi.academico.dto.mensagem;

import java.util.UUID;

/** Projeção de MensagemProjetoRepository.contarNaoLidas. */
public record NaoLidasPorProjeto(UUID projetoId, Long total) {
}
