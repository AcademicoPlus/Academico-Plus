package com.unifapi.academico.domain.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.unifapi.academico.domain.enums.StatusProjeto;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Entity
@Table(name = "projetos", indexes = {
    @Index(name = "idx_projetos_criador", columnList = "criador_id"),
    @Index(name = "idx_projetos_status", columnList = "status"),
    @Index(name = "idx_projetos_ativo", columnList = "ativo"),
    @Index(name = "idx_projetos_criado_em", columnList = "criado_em")
})
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criador_id", nullable = false)
    private Usuario criador;

    @NotBlank(message = "Título é obrigatório")
    @Size(min = 5, max = 255, message = "Título deve ter entre 5 e 255 caracteres")
    @Column(nullable = false)
    private String titulo;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(min = 20, message = "Descrição deve ter no mínimo 20 caracteres")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusProjeto status = StatusProjeto.ABERTO;

    @NotNull(message = "Número de vagas é obrigatório")
    @Min(value = 1, message = "Deve ter no mínimo 1 vaga")
    @Column(nullable = false)
    private Integer vagas;

    @Column(name = "vagas_preenchidas")
    @Builder.Default
    private Integer vagasPreenchidas = 0;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    // ============================================
    // RELACIONAMENTOS
    // ============================================

    /**
     * BatchSize evita N+1 nas listagens paginadas: como é uma coleção @OneToMany,
     * não pode entrar no @EntityGraph de queries com LIMIT/OFFSET (Hibernate
     * paginaria em memória). Com batching, o lazy load de várias entidades da
     * mesma página vira poucas queries "IN (...)" em vez de uma por projeto.
     */
    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private Set<ProjetoHabilidade> habilidadesNecessarias = new HashSet<>();

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ProjetoMembro> membros = new HashSet<>();

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Candidatura> candidaturas = new HashSet<>();

    // ============================================
    // MÉTODOS HELPER
    // ============================================

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public boolean podeAceitarCandidato() {
        return vagasPreenchidas < vagas && status == StatusProjeto.ABERTO;
    }
}