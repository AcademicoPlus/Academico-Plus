package com.unifapi.academico.dto.avaliacao;

import com.unifapi.academico.domain.models.Avaliacao;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.projeto.ProjetoRefResponse;

import java.time.LocalDateTime;
import java.util.UUID;

/** Usado em "avaliações recebidas por X" — o avaliado já é o contexto da URL, por isso não aparece aqui. */
public record AvaliacaoResponse(
        UUID id,
        ProjetoRefResponse projeto,
        UsuarioResumoResponse avaliador,
        Integer nota,
        String comentario,
        LocalDateTime criadoEm
) {
    public static AvaliacaoResponse fromEntity(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.getId(),
                ProjetoRefResponse.fromEntity(avaliacao.getProjeto()),
                UsuarioResumoResponse.fromEntity(avaliacao.getAvaliador()),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getCriadoEm()
        );
    }
}
