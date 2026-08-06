package com.unifapi.academico.domain.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString(exclude = {"nivelAcesso", "curso", "habilidades", "projetosCriados", "projetos", "candidaturas"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuarios_email", columnList = "email"),
    @Index(name = "idx_usuarios_ativo", columnList = "ativo")
})
// Configurações do Soft Delete (Hibernate 6)
@SQLDelete(sql = "UPDATE usuarios SET ativo = false, deletado_em = NOW() WHERE id = ?")
@SQLRestriction("ativo = true")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nome;

    @Column
    private Integer periodo;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    // ============================================
    // CAMPOS DE RECUPERAÇÃO DE SENHA (OTP)
    // ============================================

    @Column(name = "codigo_recuperacao")
    private String codigoRecuperacao;

    @Column(name = "codigo_recuperacao_expira_em")
    private LocalDateTime codigoRecuperacaoExpiraEm;

    // ============================================
    // TIMESTAMPS E SOFT DELETE
    // ============================================

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "deletado_em")
    private LocalDateTime deletadoEm;

    // ============================================
    // RELACIONAMENTOS DE DOMÍNIO
    // ============================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_acesso_id", nullable = false)
    private NivelAcesso nivelAcesso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    /** BatchSize evita N+1 caso este campo um dia seja lido a partir de uma listagem paginada. */
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private Set<UsuarioHabilidade> habilidades = new HashSet<>();

    @OneToMany(mappedBy = "criador", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Projeto> projetosCriados = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ProjetoMembro> projetos = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Candidatura> candidaturas = new HashSet<>();

    // ============================================
    // CICLO DE VIDA DO JPA
    // ============================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // ============================================
    // MÉTODOS HELPER
    // ============================================

    public void adicionarHabilidade(UsuarioHabilidade habilidade) {
        habilidades.add(habilidade);
        habilidade.setUsuario(this);
    }

    public void removerHabilidade(UsuarioHabilidade habilidade) {
        habilidades.remove(habilidade);
        habilidade.setUsuario(null);
    }
}