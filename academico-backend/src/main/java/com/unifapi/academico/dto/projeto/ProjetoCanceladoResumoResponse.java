package com.unifapi.academico.dto.projeto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.UUID;

public record ProjetoCanceladoResumoResponse(
        UUID id,
        String nome,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate data,

        String tipo
) {}