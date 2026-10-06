package com.unifapi.academico.dto.usuario;

import com.unifapi.academico.domain.models.NivelAcesso;

import java.util.UUID;

public record NivelAcessoResponse(
    UUID id,
    String nome,
    String descricao
) {
    public static NivelAcessoResponse fromEntity(NivelAcesso nivelAcesso) {
        if (nivelAcesso == null) return null;
        
        return new NivelAcessoResponse(
            nivelAcesso.getId(),
            nivelAcesso.getNome(),
            nivelAcesso.getDescricao()
        );
    }
}