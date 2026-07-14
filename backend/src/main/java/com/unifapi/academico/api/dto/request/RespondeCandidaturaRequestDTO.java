package com.unifapi.academico.api.dto.request;


import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespondeCandidaturaRequestDTO {
    
    @Size(max = 500, message = "Motivo não pode ter mais de 500 caracteres")
    private String motivo;
}