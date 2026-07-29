package com.unifapi.academico.domain.model;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Getter;   
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "projeto_habilidades", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"projeto_id", "habilidade_id"})
}, indexes = {
    @Index(name = "idx_projeto_habilidades_projeto", columnList = "projeto_id"),
    @Index(name = "idx_projeto_habilidades_habilidade", columnList = "habilidade_id")
})
public class ProjetoHabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    @EqualsAndHashCode.Include
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_id", nullable = false)
    @EqualsAndHashCode.Include 
    private Habilidade habilidade;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obrigatoria = false;
}