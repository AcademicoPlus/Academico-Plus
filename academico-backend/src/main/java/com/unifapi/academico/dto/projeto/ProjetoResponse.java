package com.unifapi.academico.dto.projeto;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjetoResponse(
        UUID id,
        UsuarioResumoResponse criador,
        String titulo,
        String descricao,
        StatusProjeto status,
        Integer vagas,
        Integer vagasPreenchidas,
        LocalDateTime dataFim,
        Boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static ProjetoResponse fromEntity(Projeto projeto) {
        return new ProjetoResponse(
                projeto.getId(),
                projeto.getCriador() != null ? UsuarioResumoResponse.fromEntity(projeto.getCriador()) : null,
                projeto.getTitulo(),
                projeto.getDescricao(),
                projeto.getStatus(),
                projeto.getVagas(),
                projeto.getVagasPreenchidas(),
                projeto.getDataFim(),
                projeto.getAtivo(),
                projeto.getCriadoEm(),
                projeto.getAtualizadoEm()
        );
    }
}