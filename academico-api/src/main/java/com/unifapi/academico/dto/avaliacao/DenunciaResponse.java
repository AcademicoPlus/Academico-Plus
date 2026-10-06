package com.unifapi.academico.dto.avaliacao;

import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.models.DenunciaAvaliacao;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record DenunciaResponse(
        UUID id,
        UUID avaliacaoId,
        UsuarioResumoResponse avaliador,
        Integer notaAvaliacao,
        String comentarioAvaliacao,
        UsuarioResumoResponse denunciante,
        String motivo,
        StatusDenuncia status,
        LocalDateTime criadoEm,
        LocalDateTime analisadoEm
) {
    public static DenunciaResponse fromEntity(DenunciaAvaliacao denuncia) {
        var avaliacao = denuncia.getAvaliacao();
        return new DenunciaResponse(
                denuncia.getId(),
                avaliacao.getId(),
                UsuarioResumoResponse.fromEntity(avaliacao.getAvaliador()),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                UsuarioResumoResponse.fromEntity(denuncia.getDenunciante()),
                denuncia.getMotivo(),
                denuncia.getStatus(),
                denuncia.getCriadoEm(),
                denuncia.getAnalisadoEm()
        );
    }
}
