package com.unifapi.academico.dto.denuncia;

import com.unifapi.academico.domain.models.Avaliacao;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;

import java.util.UUID;

/**
 * O que foi denunciado, achatado num formato único para a fila do admin
 * renderizar os três tipos de alvo com o mesmo card.
 */
public record AlvoDenunciadoResponse(
        UUID id,
        String titulo,
        String conteudo,
        /** Só em avaliação; nulo nos demais alvos. */
        Integer nota,
        /** Quem publicou o conteúdo — é o responsável que pode ser sancionado. */
        UsuarioResumoResponse autor
) {

    public static AlvoDenunciadoResponse deAvaliacao(Avaliacao avaliacao) {
        return new AlvoDenunciadoResponse(
                avaliacao.getId(),
                "Avaliação de " + nomeOuVazio(avaliacao.getAvaliador()),
                avaliacao.getComentario(),
                avaliacao.getNota(),
                resumo(avaliacao.getAvaliador())
        );
    }

    public static AlvoDenunciadoResponse deProjeto(Projeto projeto) {
        return new AlvoDenunciadoResponse(
                projeto.getId(),
                projeto.getTitulo(),
                projeto.getDescricao(),
                null,
                resumo(projeto.getCriador())
        );
    }

    public static AlvoDenunciadoResponse dePerfil(Usuario usuario) {
        return new AlvoDenunciadoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getBio(),
                null,
                resumo(usuario)
        );
    }

    private static UsuarioResumoResponse resumo(Usuario usuario) {
        return usuario != null ? UsuarioResumoResponse.fromEntity(usuario) : null;
    }

    private static String nomeOuVazio(Usuario usuario) {
        return usuario != null ? usuario.getNome() : "usuário removido";
    }
}
