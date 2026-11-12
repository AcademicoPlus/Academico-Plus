package com.unifapi.academico.dto.usuario;

import java.time.LocalDateTime;

/**
 * @param token novo token de acesso: trocar a senha invalida todos os tokens
 *              emitidos antes, inclusive o da sessão que fez a troca — o front
 *              deve substituir o token salvo por este.
 */
public record UsuarioUpdateSenhaResponse(
    String mensagem,
    LocalDateTime dataAtualizacao,
    String token
) {
    public static UsuarioUpdateSenhaResponse sucesso(String token) {
        return new UsuarioUpdateSenhaResponse(
            "Senha alterada com sucesso.",
            LocalDateTime.now(),
            token
        );
    }
}
