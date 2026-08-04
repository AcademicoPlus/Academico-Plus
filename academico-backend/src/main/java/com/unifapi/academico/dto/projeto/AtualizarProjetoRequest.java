package com.unifapi.academico.dto.projeto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AtualizarProjetoRequest(

        @NotBlank(message = "Título é obrigatório")
        @Size(min = 5, max = 255, message = "Título deve ter entre 5 e 255 caracteres")
        String titulo,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(min = 20, message = "Descrição deve ter no mínimo 20 caracteres")
        String descricao,

        @NotNull(message = "Número de vagas é obrigatório")
        @Min(value = 1, message = "Deve ter no mínimo 1 vaga")
        Integer vagas,

        LocalDateTime dataFim
) {
}