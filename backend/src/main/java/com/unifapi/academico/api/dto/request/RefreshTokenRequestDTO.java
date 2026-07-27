package com.unifapi.academico.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequestDTO {

    @NotBlank(message = "O Refresh Token é obrigatório")
    private String refreshToken;
}