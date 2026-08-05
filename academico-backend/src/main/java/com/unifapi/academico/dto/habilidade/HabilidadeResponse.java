package com.unifapi.academico.dto.habilidade;

import com.unifapi.academico.domain.models.Habilidade;

import java.time.LocalDateTime;
import java.util.UUID;

public record HabilidadeResponse(
        UUID id,
        String nome,
        String categoria,
        String descricao,
        Integer usuariosCount,
        Integer projetosCount,
        LocalDateTime criadoEm
) {
    public static HabilidadeResponse fromEntity(Habilidade habilidade) {
        return new HabilidadeResponse(
                habilidade.getId(),
                habilidade.getNome(),
                habilidade.getCategoria(),
                habilidade.getDescricao(),
                habilidade.getUsuariosCount(),
                habilidade.getProjetosCount(),
                habilidade.getCriadoEm()
        );
    }
}
