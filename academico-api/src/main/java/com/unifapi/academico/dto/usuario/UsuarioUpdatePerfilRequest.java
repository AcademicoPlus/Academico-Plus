package com.unifapi.academico.dto.usuario;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/**
 * Sem fotoUrl de propósito: a foto só muda por POST/DELETE /usuarios/me/foto,
 * que passam pelo upload-service. Aceitar a URL livre aqui permitia gravar a
 * URL da foto (ou da capa de projeto) de outra pessoa e, em seguida, chamar
 * DELETE /usuarios/me/foto para apagar esse arquivo do R2.
 */
public record UsuarioUpdatePerfilRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    String nome,

    UUID idCurso,

    Integer periodo,

    String bio,

    String linkedinUrl,

    String githubUrl
) {}