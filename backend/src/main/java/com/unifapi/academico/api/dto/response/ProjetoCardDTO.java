package com.unifapi.academico.api.dto.response;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.model.Projeto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoCardDTO {
    
    private Long id;
    private String titulo;
    private String descricao;
    private StatusProjeto status;
    private Integer vagas;
    private Integer vagasPreenchidas;
    private LocalDateTime dataFim;
    private UserDTO criador;
    private List<String> habilidades;

    /**
     * Visão Resumida (Item): Retorna apenas os dados essenciais de um projeto para ser exibido em cards/listas.
     */
    public static ProjetoCardDTO from(Projeto projeto) {
        if (projeto == null) {
            return null;
        }

        List<String> habilidades = projeto.getHabilidadesNecessarias().stream()
            .map(ph -> ph.getHabilidade().getNome())
            .toList();

        return ProjetoCardDTO.builder()
            .id(projeto.getId())
            .titulo(projeto.getTitulo())
            .descricao(projeto.getDescricao())
            .status(projeto.getStatus())
            .vagas(projeto.getVagas())
            .vagasPreenchidas(projeto.getVagasPreenchidas())
            .dataFim(projeto.getDataFim())
            .criador(UserDTO.from(projeto.getCriador()))
            .habilidades(habilidades)
            .build();
    }
}