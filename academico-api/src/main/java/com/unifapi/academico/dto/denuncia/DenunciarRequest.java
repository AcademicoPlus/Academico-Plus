package com.unifapi.academico.dto.denuncia;

import com.unifapi.academico.domain.enums.CategoriaDenuncia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de qualquer denúncia — de avaliação, de projeto ou de perfil. */
public record DenunciarRequest(

        @NotNull(message = "A categoria da denúncia é obrigatória")
        CategoriaDenuncia categoria,

        @NotBlank(message = "O motivo da denúncia é obrigatório")
        @Size(min = 10, max = 1000, message = "Motivo deve ter entre 10 e 1000 caracteres")
        String motivo
) {
}
