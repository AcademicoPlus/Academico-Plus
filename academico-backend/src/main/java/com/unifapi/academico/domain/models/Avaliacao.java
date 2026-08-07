package com.unifapi.academico.domain.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Avaliação de um participante por outro, ambos do mesmo projeto. Só é
 * permitida depois que o projeto chega a um estado final (ver AvaliacaoService),
 * para não virar ferramenta de retaliação no meio da colaboração.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "avaliacoes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"projeto_id", "avaliador_id", "avaliado_id"})
}, indexes = {
        @Index(name = "idx_avaliacoes_projeto", columnList = "projeto_id"),
        @Index(name = "idx_avaliacoes_avaliado", columnList = "avaliado_id"),
        @Index(name = "idx_avaliacoes_avaliador", columnList = "avaliador_id")
})
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliador_id", nullable = false)
    private Usuario avaliador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliado_id", nullable = false)
    private Usuario avaliado;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
