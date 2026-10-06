package com.unifapi.academico.dto.usuario;

import com.unifapi.academico.domain.models.Curso;

import java.time.LocalDateTime;
import java.util.UUID;

public record CursoResponse(
    UUID id,
    String nome,
    LocalDateTime criadoEm
) {
    public static CursoResponse fromEntity(Curso curso) {
        if (curso == null) return null;

        return new CursoResponse(
            curso.getId(),
            curso.getNome(),
            curso.getCriadoEm()
        );
    }
}