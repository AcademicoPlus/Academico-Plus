package com.unifapi.academico.api.dto.response;

import java.util.UUID;

import com.unifapi.academico.domain.enums.TipoUsuario;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private UUID id;
    private String email;
    private String nome;
    private TipoUsuario tipo;
    private String token;         // Access Token (JWT curto)
    private String refreshToken;  // Refresh Token (Longa duração)
    private Long expiresIn;       // Ex: 900000ms (15min)
}