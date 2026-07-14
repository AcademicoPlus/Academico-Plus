package com.unifapi.academico.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.unifapi.academico.domain.enums.NivelHabilidade;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "endossos_professor", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"professor_id", "aluno_id", "habilidade_id"})
}, indexes = {
    @Index(name = "idx_endossos_professor", columnList = "professor_id"),
    @Index(name = "idx_endossos_aluno", columnList = "aluno_id"),
    @Index(name = "idx_endossos_habilidade", columnList = "habilidade_id")
})
public class EndossoProfessor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private Usuario professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NivelHabilidade nivel = NivelHabilidade.INTERMEDIARIO;

    @Column(name = "data_endosso", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dataEndosso = LocalDateTime.now();
}