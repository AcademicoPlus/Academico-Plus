package com.unifapi.academico.domain.models;

import com.unifapi.academico.domain.enums.CategoriaDenuncia;
import com.unifapi.academico.domain.enums.MedidaModeracao;
import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.enums.TipoAlvoDenuncia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Denúncia de conteúdo ou conduta na plataforma. O alvo pode ser uma
 * avaliação recebida, um projeto ou o perfil de outro usuário — só a FK
 * correspondente a {@link #tipoAlvo} vem preenchida (CHECK ck_denuncias_alvo_unico
 * na V10 garante isso no banco).
 *
 * Analisada por um ADMIN, que escolhe a medida da escada
 * ({@link MedidaModeracao}): arquivar, remover o conteúdo, advertir ou
 * suspender o responsável. Ver DenunciaService.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "denuncias", indexes = {
        @Index(name = "idx_denuncias_avaliacao", columnList = "avaliacao_id"),
        @Index(name = "idx_denuncias_status", columnList = "status"),
        @Index(name = "idx_denuncias_tipo_alvo", columnList = "tipo_alvo")
})
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alvo", nullable = false, length = 20)
    private TipoAlvoDenuncia tipoAlvo;

    // ── Alvos: exatamente um é preenchido, conforme tipoAlvo ──────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliacao_id")
    private Avaliacao avaliacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_denunciado_id")
    private Usuario usuarioDenunciado;

    // ──────────────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CategoriaDenuncia categoria = CategoriaDenuncia.OUTRO;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusDenuncia status = StatusDenuncia.PENDENTE;

    /** Medida escolhida pelo admin na resolução; nula enquanto PENDENTE. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MedidaModeracao medida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analisado_por_id")
    private Usuario analisadoPor;

    @Column(name = "analisado_em")
    private LocalDateTime analisadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    /**
     * Quem responde pelo conteúdo denunciado — é quem recebe advertência ou
     * suspensão quando a denúncia é procedente. Para uma avaliação abusiva é
     * quem a escreveu (não quem a recebeu, que é justamente o denunciante).
     */
    public Usuario getResponsavel() {
        return switch (tipoAlvo) {
            case AVALIACAO -> avaliacao != null ? avaliacao.getAvaliador() : null;
            case PROJETO -> projeto != null ? projeto.getCriador() : null;
            case PERFIL -> usuarioDenunciado;
        };
    }
}
