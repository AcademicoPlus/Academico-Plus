package com.unifapi.academico.dto.projeto;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Projeto;

import java.util.UUID;

/**
 * Referência mínima a um projeto, usada quando o projeto aparece aninhado
 * em outro recurso (candidatura, membro) e o payload completo seria desnecessário.
 */
public record ProjetoRefResponse(
        UUID id,
        String titulo,
        StatusProjeto status
) {
    public static ProjetoRefResponse fromEntity(Projeto projeto) {
        if (projeto == null) {
            return null;
        }
        return new ProjetoRefResponse(
                projeto.getId(),
                projeto.getTitulo(),
                projeto.getStatus()
        );
    }
}
