package com.unifapi.academico.dto.mensagem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensagemRequest(
        @NotBlank(message = "A mensagem não pode ser vazia.")
        @Size(max = 4000, message = "A mensagem deve ter no máximo 4000 caracteres.")
        String conteudo
) {
}
