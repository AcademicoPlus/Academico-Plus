package com.unifapi.academico.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.unifapi.academico.domain.enums.StatusCandidatura;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "candidaturas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"projeto_id", "usuario_id"})
}, indexes = {
    @Index(name = "idx_candidaturas_projeto", columnList = "projeto_id"),
    @Index(name = "idx_candidaturas_usuario", columnList = "usuario_id"),
    @Index(name = "idx_candidaturas_status", columnList = "status")
})
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusCandidatura status = StatusCandidatura.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "motivo_rejeicao", columnDefinition = "TEXT")
    private String motivoRejeicao;

    @Column(name = "data_candidatura", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dataCandidatura = LocalDateTime.now();

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;
}