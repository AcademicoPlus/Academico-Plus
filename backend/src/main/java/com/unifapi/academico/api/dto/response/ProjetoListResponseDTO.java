package com.unifapi.academico.api.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;


import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoListResponseDTO {
    
    private Integer pagina;
    private Integer limite;
    private Long total;
    private Integer totalPaginas;
    private List<ProjetoListDTO> dados;

    /**
     * Converter Page<Projeto> para ProjetoListResponseDTO
     */
    public static ProjetoListResponseDTO from(Page<?> page, List<ProjetoListDTO> dados) {
        return ProjetoListResponseDTO.builder()
            .pagina(page.getNumber() + 1)  // Spring usa 0-indexed
            .limite(page.getSize())
            .total(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .dados(dados)
            .build();
    }
}