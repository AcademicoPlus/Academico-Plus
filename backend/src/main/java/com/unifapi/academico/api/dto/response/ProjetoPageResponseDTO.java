package com.unifapi.academico.api.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;


import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoPageResponseDTO {
    
    private Integer pagina;
    private Integer limite;
    private Long total;
    private Integer totalPaginas;
    private List<ProjetoCardDTO> dados;

    /**
     * Converter Page<Projeto> para ProjetoListResponseDTO
     */
    public static ProjetoPageResponseDTO from(Page<?> page, List<ProjetoCardDTO> dados) {
        return ProjetoPageResponseDTO.builder()
            .pagina(page.getNumber() + 1)  
            .limite(page.getSize())
            .total(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .dados(dados)
            .build();
    }
}