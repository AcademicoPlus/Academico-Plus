package com.unifapi.academico.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHabilidadeRequestDTO {
    
    @NotBlank(message = "Nome da habilidade é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String nome;
    
    @Size(max = 100, message = "Categoria não pode ter mais de 100 caracteres")
    private String categoria;
    
    @Size(max = 500, message = "Descrição não pode ter mais de 500 caracteres")
    private String descricao;
}