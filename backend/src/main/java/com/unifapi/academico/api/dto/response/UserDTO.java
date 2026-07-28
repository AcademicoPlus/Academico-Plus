package com.unifapi.academico.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.domain.model.Usuario;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private UUID id;
    private String email;
    private String nome;
    private TipoUsuario userType;
    private String academicInfo;
    private String bio;
    private String profilePhotoUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserDTO from(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return UserDTO.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .userType(usuario.getTipo())
                .academicInfo(usuario.getCurso())
                .bio(usuario.getBio())
                .profilePhotoUrl(usuario.getFotoUrl())
                .isActive(usuario.getAtivo())
                .createdAt(usuario.getCriadoEm())
                .updatedAt(usuario.getAtualizadoEm())
                .build();
    }
}