package com.unifapi.academico.domain.models;

import com.unifapi.academico.domain.enums.StatusDenuncia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Denúncia feita pelo avaliado contra uma Avaliacao recebida, quando considera
 * o conteúdo abusivo, falso ou fora do propósito de avaliar a colaboração.
 * Analisada por um ADMIN, que decide se remove a avaliação (ver DenunciaService).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "denuncias_avaliacao", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"avaliacao_id", "denunciante_id"})
}, indexes = {
        @Index(name = "idx_denuncias_avaliacao", columnList = "avaliacao_id"),
        @Index(name = "idx_denuncias_status", columnList = "status")
})
public class DenunciaAvaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliacao_id", nullable = false)
    private Avaliacao avaliacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusDenuncia status = StatusDenuncia.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analisado_por_id")
    private Usuario analisadoPor;

    @Column(name = "analisado_em")
    private LocalDateTime analisadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
