package com.unifapi.academico.dto.membro;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Adição direta de membro pelo criador do projeto (sem passar por candidatura). */
public record AdicionarMembroRequest(

        @NotNull(message = "ID do usuário é obrigatório")
        UUID usuarioId,

        @Size(max = 100, message = "Função deve ter no máximo 100 caracteres")
        String funcao
) {
}
