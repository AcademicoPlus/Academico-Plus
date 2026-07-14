package com.unifapi.academico.api.dto.response;

import com.unifapi.academico.domain.model.Candidatura;
import com.unifapi.academico.domain.enums.StatusCandidatura;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidaturaDTO {
    
    private Long id;
    private Long projetoId;
    private String projetoTitulo;
    private Long usuarioId;
    private String usuarioNome;
    private StatusCandidatura status;
    private String mensagem;
    private String motivoRejeicao;
    private LocalDateTime dataCandidatura;
    private LocalDateTime dataResposta;

    /**
     * Converter Candidatura para DTO
     */
    public static CandidaturaDTO from(Candidatura candidatura) {
        if (candidatura == null) {
            return null;
        }

        return CandidaturaDTO.builder()
            .id(candidatura.getId())
            .projetoId(candidatura.getProjeto().getId())
            .projetoTitulo(candidatura.getProjeto().getTitulo())
            .usuarioId(candidatura.getUsuario().getId())
            .usuarioNome(candidatura.getUsuario().getNome())
            .status(candidatura.getStatus())
            .mensagem(candidatura.getMensagem())
            .motivoRejeicao(candidatura.getMotivoRejeicao())
            .dataCandidatura(candidatura.getDataCandidatura())
            .dataResposta(candidatura.getDataResposta())
            .build();
    }
}