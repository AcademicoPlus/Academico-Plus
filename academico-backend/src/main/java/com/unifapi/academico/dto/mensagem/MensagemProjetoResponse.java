package com.unifapi.academico.dto.mensagem;

import com.unifapi.academico.domain.models.MensagemProjeto;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record MensagemProjetoResponse(
        UUID id,
        UUID projetoId,
        UsuarioResumoResponse autor,
        String conteudo,
        LocalDateTime criadoEm
) {
    public static MensagemProjetoResponse fromEntity(MensagemProjeto mensagem) {
        return new MensagemProjetoResponse(
                mensagem.getId(),
                mensagem.getProjeto().getId(),
                UsuarioResumoResponse.fromEntity(mensagem.getAutor()),
                mensagem.getConteudo(),
                mensagem.getCriadoEm()
        );
    }
}
