package com.unifapi.academico.api.dto.response;

import com.unifapi.academico.domain.enums.StatusProjeto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
//Visão Completa: Retorna todos os detalhes de um único projeto (GET /api/projetos/{id}). Inclui membros, candidatos, etc.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoDetailResponseDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private StatusProjeto status;
    private Integer vagas;
    private Integer vagasPreenchidas;
    private LocalDateTime dataFim;
    private LocalDateTime criadoEm;
    
    private UserDTO criador;
    private List<String> habilidadesNecessarias;
    private List<UserDTO> membros;
    private Integer totalCandidatos;
}