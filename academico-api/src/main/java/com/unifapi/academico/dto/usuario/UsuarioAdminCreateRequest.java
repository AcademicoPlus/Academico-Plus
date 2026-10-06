package com.unifapi.academico.dto.usuario;

import jakarta.validation.constraints.NotBlank;

public record UsuarioAdminCreateRequest(

    @NotBlank
    String nome,

    @NotBlank
    String email,

    @NotBlank
    String curso,

    @NotBlank
    String senha,

    @NotBlank
    String permission



) {

}
