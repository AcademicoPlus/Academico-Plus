package com.unifapi.academico.domain.models;

import com.unifapi.academico.domain.enums.MedidaModeracao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Advertência ou suspensão aplicada a um usuário por um ADMIN ao julgar uma
 * denúncia procedente. Acumula histórico: é contando as advertências
 * anteriores que a fila sugere subir para a suspensão.
 *
 * Também é o canal de entrega — a sanção ainda não vista volta no login
 * (AuthService.consumirSancoes), mesmo padrão do aviso de projeto cancelado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sancoes", indexes = {
        @Index(name = "idx_sancoes_usuario", columnList = "usuario_id"),
        @Index(name = "idx_sancoes_medida", columnList = "medida")
})
public class Sancao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** A denúncia que motivou a sanção. Nula só se a denúncia for apagada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denuncia_id")
    private Denuncia denuncia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedidaModeracao medida;

    /** Texto que o usuário lê no login — precisa dizer o que ele fez de errado. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String justificativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aplicado_por_id", nullable = false)
    private Usuario aplicadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    /** Fim da suspensão. Nulo em advertência (que não expira, só acumula). */
    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    @Column(name = "visualizado_em")
    private LocalDateTime visualizadoEm;
}
