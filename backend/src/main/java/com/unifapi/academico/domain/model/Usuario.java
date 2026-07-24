package com.unifapi.academico.domain.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.unifapi.academico.domain.enums.TipoUsuario;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuarios_email", columnList = "email"),
    @Index(name = "idx_usuarios_tipo", columnList = "tipo"),
    @Index(name = "idx_usuarios_ativo", columnList = "ativo")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    @Column(nullable = false)
    private String nome;

    @Column
    private String curso;

    @Column
    private Integer periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TipoUsuario tipo = TipoUsuario.ALUNO;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @Column(nullable = false)
    private String senhaHash;

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

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
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

    @OneToMany(mappedBy = "professor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<EndossoProfessor> endossosFeitos = new HashSet<>();

    @OneToMany(mappedBy = "aluno", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<EndossoProfessor> endossosRecebidos = new HashSet<>();

    // ============================================
    // MÉTODOS HELPER
    // ============================================

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public void adicionarHabilidade(UsuarioHabilidade habilidade) {
        habilidades.add(habilidade);
        habilidade.setUsuario(this);
    }

    public void removerHabilidade(UsuarioHabilidade habilidade) {
        habilidades.remove(habilidade);
        habilidade.setUsuario(null);
    }
}