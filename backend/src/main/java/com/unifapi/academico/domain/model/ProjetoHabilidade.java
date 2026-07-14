package com.unifapi.academico.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "projeto_habilidades", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"projeto_id", "habilidade_id"})
}, indexes = {
    @Index(name = "idx_projeto_habilidades_projeto", columnList = "projeto_id"),
    @Index(name = "idx_projeto_habilidades_habilidade", columnList = "habilidade_id")
})
public class ProjetoHabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obrigatoria = false;
}