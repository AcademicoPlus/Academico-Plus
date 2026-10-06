package com.unifapi.academico.domain.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Marca até quando um participante leu o chat de um projeto. Uma linha por
 * (projeto, usuário), atualizada ao abrir a conversa — ver
 * ChatProjetoService.marcarComoLida.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "leituras_chat_projeto", uniqueConstraints = {
    @UniqueConstraint(name = "uk_leitura_chat_projeto_usuario", columnNames = {"projeto_id", "usuario_id"})
}, indexes = {
    @Index(name = "idx_leituras_chat_projeto_usuario", columnList = "usuario_id")
})
public class LeituraChatProjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "lido_ate", nullable = false)
    private LocalDateTime lidoAte;
}
