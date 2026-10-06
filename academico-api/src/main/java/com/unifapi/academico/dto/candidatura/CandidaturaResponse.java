package com.unifapi.academico.dto.candidatura;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.models.Candidatura;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.projeto.ProjetoRefResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record CandidaturaResponse(
        UUID id,
        ProjetoRefResponse projeto,
        UsuarioResumoResponse usuario,
        StatusCandidatura status,
        String mensagem,
        String motivoRejeicao,
        LocalDateTime dataCandidatura,
        LocalDateTime dataResposta
) {
    public static CandidaturaResponse fromEntity(Candidatura candidatura) {
        return new CandidaturaResponse(
                candidatura.getId(),
                ProjetoRefResponse.fromEntity(candidatura.getProjeto()),
                candidatura.getUsuario() != null
                        ? UsuarioResumoResponse.fromEntity(candidatura.getUsuario())
                        : null,
                candidatura.getStatus(),
                candidatura.getMensagem(),
                candidatura.getMotivoRejeicao(),
                candidatura.getDataCandidatura(),
                candidatura.getDataResposta()
        );
    }
}
