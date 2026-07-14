package com.unifapi.academico.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCandidaturaRequestDTO {
    
    @NotBlank(message = "Mensagem de candidatura é obrigatória")
    @Size(min = 10, max = 1000, message = "Mensagem deve ter entre 10 e 1000 caracteres")
    private String mensagem;
}