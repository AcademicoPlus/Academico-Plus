package com.unifapi.academico.dto.convite;

import com.unifapi.academico.domain.enums.StatusConvite;
import com.unifapi.academico.domain.models.ConviteProjeto;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.projeto.ProjetoRefResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConviteResponse(
        UUID id,
        ProjetoRefResponse projeto,
        UsuarioResumoResponse convidado,
        UsuarioResumoResponse convidante,
        String funcao,
        String mensagem,
        StatusConvite status,
        LocalDateTime criadoEm,
        LocalDateTime respondidoEm
) {
    public static ConviteResponse fromEntity(ConviteProjeto convite) {
        return new ConviteResponse(
                convite.getId(),
                ProjetoRefResponse.fromEntity(convite.getProjeto()),
                convite.getConvidado() != null ? UsuarioResumoResponse.fromEntity(convite.getConvidado()) : null,
                convite.getConvidante() != null ? UsuarioResumoResponse.fromEntity(convite.getConvidante()) : null,
                convite.getFuncao(),
                convite.getMensagem(),
                convite.getStatus(),
                convite.getCriadoEm(),
                convite.getRespondidoEm()
        );
    }
}
