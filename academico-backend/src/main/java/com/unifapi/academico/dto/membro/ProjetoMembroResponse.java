package com.unifapi.academico.dto.membro;

import com.unifapi.academico.domain.models.ProjetoMembro;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.projeto.ProjetoRefResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjetoMembroResponse(
        UUID id,
        ProjetoRefResponse projeto,
        UsuarioResumoResponse usuario,
        String funcao,
        LocalDateTime dataAdesao
) {
    /** Usado nas listagens por projeto, onde o projeto já é o contexto da URL. */
    public static ProjetoMembroResponse fromEntity(ProjetoMembro membro) {
        return new ProjetoMembroResponse(
                membro.getId(),
                null,
                membro.getUsuario() != null ? UsuarioResumoResponse.fromEntity(membro.getUsuario()) : null,
                membro.getFuncao(),
                membro.getDataAdesao()
        );
    }

    /** Usado quando o membro é retornado fora do contexto de um projeto específico. */
    public static ProjetoMembroResponse fromEntityComProjeto(ProjetoMembro membro) {
        return new ProjetoMembroResponse(
                membro.getId(),
                ProjetoRefResponse.fromEntity(membro.getProjeto()),
                membro.getUsuario() != null ? UsuarioResumoResponse.fromEntity(membro.getUsuario()) : null,
                membro.getFuncao(),
                membro.getDataAdesao()
        );
    }
}
