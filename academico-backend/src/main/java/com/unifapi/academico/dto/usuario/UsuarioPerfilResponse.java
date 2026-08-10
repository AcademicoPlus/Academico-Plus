package com.unifapi.academico.dto.usuario;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.habilidade.UsuarioHabilidadeResponse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
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
    List<UsuarioHabilidadeResponse> habilidades,
    Double notaMedia,
    Integer totalAvaliacoes,
    LocalDateTime termosAceitosEm,
    Boolean ativo,
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm
) {
    public static UsuarioPerfilResponse fromEntity(Usuario usuario) {
        List<UsuarioHabilidadeResponse> habilidades = usuario.getHabilidades() == null
                ? List.of()
                : usuario.getHabilidades().stream()
                        .map(UsuarioHabilidadeResponse::fromEntity)
                        // Set não tem ordem definida: ordena para deixar o payload estável
                        .sorted(Comparator.comparing(
                                h -> h.habilidade() != null ? h.habilidade().nome() : "",
                                String.CASE_INSENSITIVE_ORDER))
                        .toList();

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
            habilidades,
            usuario.getNotaMedia(),
            usuario.getTotalAvaliacoes(),
            usuario.getTermosAceitosEm(),
            usuario.getAtivo(),
            usuario.getCriadoEm(),
            usuario.getAtualizadoEm()
        );
    }
}