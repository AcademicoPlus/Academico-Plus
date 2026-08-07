package com.unifapi.academico.dto.habilidade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarHabilidadeRequest(

        @NotBlank(message = "Nome da habilidade é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String nome,

        @Size(max = 100, message = "Categoria deve ter no máximo 100 caracteres")
        String categoria,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        String descricao
) {
}
