package com.unifapi.academico.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoRequestDTO {

    @NotBlank(message = "O título é obrigatório")
    private String titulo;

    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 20, message = "Descrição deve ter no mínimo 20 caracteres")
    private String descricao;

    @NotNull(message = "O número de vagas é obrigatório")
    @Min(value = 1, message = "O projeto deve ter no mínimo 1 vaga")
    private Integer vagas;

    private LocalDateTime dataFim;

    private List<UUID> habilidadeIds;
}