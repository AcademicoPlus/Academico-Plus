package com.unifapi.academico.dto.candidatura;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CriarCandidaturaRequest(

        @NotNull(message = "ID do projeto é obrigatório")
        UUID projetoId,

        @Size(max = 1000, message = "Mensagem deve ter no máximo 1000 caracteres")
        String mensagem
) {
}
