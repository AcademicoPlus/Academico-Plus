package com.unifapi.academico.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.unifapi.academico.domain.enums.NivelHabilidade;

import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "usuario_habilidades", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "habilidade_id"})
}, indexes = {
    @Index(name = "idx_usuario_habilidades_usuario", columnList = "usuario_id"),
    @Index(name = "idx_usuario_habilidades_habilidade", columnList = "habilidade_id")
})
public class UsuarioHabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

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


    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}