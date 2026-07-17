package com.unifapi.academico.api.dto.request;

import com.unifapi.academico.domain.enums.NivelHabilidade;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddHabilidadeRequestDTO {
    
    @NotNull(message = "ID da habilidade é obrigatório")
    private Long habilidadeId;
    
    @NotNull(message = "Nível é obrigatório")
    private NivelHabilidade nivel;
}