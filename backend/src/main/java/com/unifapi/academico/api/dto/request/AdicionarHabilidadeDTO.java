package com.unifapi.academico.api.dto.request;

import java.util.UUID;

import com.unifapi.academico.domain.enums.NivelHabilidade;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdicionarHabilidadeDTO {
    
    @NotNull(message = "ID da habilidade é obrigatório")
    private UUID habilidadeId;
    
    @NotNull(message = "Nível é obrigatório")
    private NivelHabilidade nivel;
}