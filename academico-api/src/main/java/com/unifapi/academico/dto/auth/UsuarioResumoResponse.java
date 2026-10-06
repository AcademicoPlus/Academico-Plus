package com.unifapi.academico.dto.auth;

import com.unifapi.academico.domain.models.Usuario;
import java.util.UUID;

/** Perfil resumido de OUTRO usuário (membros de projeto, candidaturas, avaliações, exploração de perfis) — por isso não inclui email. */
public record UsuarioResumoResponse(
        UUID id,
        String nome,
        String curso,
        String fotoUrl,
        String permission,
        Integer periodo,
        Double notaMedia,
        Integer totalAvaliacoes
) {
    public static UsuarioResumoResponse fromEntity(Usuario usuario) {
        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCurso() != null ? usuario.getCurso().getNome() : null,
                usuario.getFotoUrl(),
                usuario.getNivelAcesso() != null ? usuario.getNivelAcesso().getNome() : null,
                usuario.getPeriodo(),
                usuario.getNotaMedia(),
                usuario.getTotalAvaliacoes()
        );
    }
}