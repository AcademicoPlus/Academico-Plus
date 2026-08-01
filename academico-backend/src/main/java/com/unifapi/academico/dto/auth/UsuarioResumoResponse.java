package com.unifapi.academico.dto.auth;

import com.unifapi.academico.domain.models.Usuario;
import java.util.UUID;

public record UsuarioResumoResponse(
        UUID id,
        String nome,
        String email,
        String curso,
        String fotoUrl,
        String permission,
        Integer periodo
) {
    public static UsuarioResumoResponse fromEntity(Usuario usuario) {
        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCurso() != null ? usuario.getCurso().getNome() : null,
                usuario.getFotoUrl(),
                usuario.getNivelAcesso() != null ? usuario.getNivelAcesso().getNome() : null,
                usuario.getPeriodo()
        );
    }
}