package com.unifapi.academico.api.dto.response;

import com.unifapi.academico.domain.model.Habilidade;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabilidadeDTO {
    
    private Long id;
    private String nome;
    private String categoria;
    private String descricao;
    private Integer usuariosCount;
    private Integer projetosCount;

    /**
     * Converter Habilidade para DTO
     */
    public static HabilidadeDTO from(Habilidade habilidade) {
        if (habilidade == null) {
            return null;
        }

        return HabilidadeDTO.builder()
            .id(habilidade.getId())
            .nome(habilidade.getNome())
            .categoria(habilidade.getCategoria())
            .descricao(habilidade.getDescricao())
            .usuariosCount(habilidade.getUsuariosCount())
            .projetosCount(habilidade.getProjetosCount())
            .build();
    }
}