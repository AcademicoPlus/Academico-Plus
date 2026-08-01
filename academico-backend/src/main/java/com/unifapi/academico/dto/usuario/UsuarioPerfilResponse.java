package com.unifapi.academico.dto.usuario;

import com.unifapi.academico.domain.models.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioPerfilResponse(
    UUID id,
    String email,
    String nome,
    NivelAcessoResponse nivelAcesso,
    CursoResponse curso,             
    Integer periodo,                 
    String bio,
    String fotoUrl,
    String linkedinUrl,
    String githubUrl,
    Boolean ativo,
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm
) {
    public static UsuarioPerfilResponse fromEntity(Usuario usuario) {
        return new UsuarioPerfilResponse(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getNome(),
            NivelAcessoResponse.fromEntity(usuario.getNivelAcesso()), 
            CursoResponse.fromEntity(usuario.getCurso()),             
            usuario.getPeriodo(),                                     
            usuario.getBio(),
            usuario.getFotoUrl(),
            usuario.getLinkedinUrl(),
            usuario.getGithubUrl(),
            usuario.getAtivo(),
            usuario.getCriadoEm(),
            usuario.getAtualizadoEm()
        );
    }
}