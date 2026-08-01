package com.unifapi.academico.dto.usuario;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record UsuarioUpdatePerfilRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    String nome,

    UUID idCurso,

    Integer periodo,

    String bio,

    String fotoUrl,

    String linkedinUrl,

    String githubUrl
) {}