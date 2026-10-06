package com.unifapi.academico.dto.usuario;

import java.time.LocalDateTime;

public record UsuarioUpdateSenhaResponse(
    String mensagem,
    LocalDateTime dataAtualizacao
) {
    public static UsuarioUpdateSenhaResponse sucesso() {
        return new UsuarioUpdateSenhaResponse(
            "Senha alterada com sucesso.", 
            LocalDateTime.now()
        );
    }
}