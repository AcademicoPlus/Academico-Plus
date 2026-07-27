package com.unifapi.academico.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDTO {

    private String nome;

    private String academicInfo; // ou curso

    @Size(max = 255, message = "A bio deve ter no máximo 255 caracteres")
    private String bio;

    private String profilePhotoUrl;
}