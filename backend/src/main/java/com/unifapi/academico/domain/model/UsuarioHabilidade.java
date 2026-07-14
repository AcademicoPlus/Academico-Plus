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
@Table(name = "usuario_habilidades", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "habilidade_id"})
}, indexes = {
    @Index(name = "idx_usuario_habilidades_usuario", columnList = "usuario_id"),
    @Index(name = "idx_usuario_habilidades_habilidade", columnList = "habilidade_id"),
    @Index(name = "idx_usuario_habilidades_endossado", columnList = "endossado")
})
public class UsuarioHabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habilidade_id", nullable = false)
    private Habilidade habilidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NivelHabilidade nivel = NivelHabilidade.INICIANTE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean endossado = false;

    @Column(name = "endossos_count")
    @Builder.Default
    private Integer endossosCount = 0;

    @Column(name = "endossado_por")
    private String endossadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}