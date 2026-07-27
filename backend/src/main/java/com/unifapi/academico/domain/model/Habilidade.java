package com.unifapi.academico.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "habilidades", indexes = {
    @Index(name = "idx_habilidades_nome", columnList = "nome"),
    @Index(name = "idx_habilidades_categoria", columnList = "categoria")
})
public class Habilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Nome da habilidade é obrigatório")
    @Column(unique = true, nullable = false, length = 100)
    private String nome;

    @Column(length = 100)
    private String categoria;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "usuarios_count")
    @Builder.Default
    private Integer usuariosCount = 0;

    @Column(name = "projetos_count")
    @Builder.Default
    private Integer projetosCount = 0;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    // ============================================
    // RELACIONAMENTOS
    // ============================================

    @OneToMany(mappedBy = "habilidade", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UsuarioHabilidade> usuarios = new HashSet<>();

    @OneToMany(mappedBy = "habilidade", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ProjetoHabilidade> projetos = new HashSet<>();

}