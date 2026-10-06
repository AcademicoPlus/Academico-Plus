package com.unifapi.academico.dto.convite;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CriarConviteRequest(

        @NotNull(message = "ID do projeto é obrigatório")
        UUID projetoId,

        @NotNull(message = "ID do usuário convidado é obrigatório")
        UUID usuarioId,

        @Size(max = 100, message = "Função deve ter no máximo 100 caracteres")
        String funcao,

        @Size(max = 1000, message = "Mensagem deve ter no máximo 1000 caracteres")
        String mensagem
) {
}
