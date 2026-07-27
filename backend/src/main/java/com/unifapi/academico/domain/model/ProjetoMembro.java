package com.unifapi.academico.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "projeto_membros", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"projeto_id", "usuario_id"})
}, indexes = {
    @Index(name = "idx_projeto_membros_projeto", columnList = "projeto_id"),
    @Index(name = "idx_projeto_membros_usuario", columnList = "usuario_id")
})
public class ProjetoMembro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 100)
    private String funcao;

    @Column(name = "data_adesao")
    @Builder.Default
    private LocalDateTime dataAdesao = LocalDateTime.now();
}