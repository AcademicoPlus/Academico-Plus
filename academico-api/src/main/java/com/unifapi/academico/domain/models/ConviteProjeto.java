package com.unifapi.academico.domain.models;

import com.unifapi.academico.domain.enums.StatusConvite;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Convite do criador do projeto para um usuário entrar na equipe — o caminho
 * inverso da Candidatura. Só pode haver um convite PENDENTE por
 * (projeto, convidado); isso é garantido por índice único parcial na
 * migration V8, já que convites respondidos ficam como histórico.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "convites_projeto", indexes = {
    @Index(name = "idx_convites_projeto_projeto", columnList = "projeto_id"),
    @Index(name = "idx_convites_projeto_convidado", columnList = "convidado_id"),
    @Index(name = "idx_convites_projeto_status", columnList = "status")
})
public class ConviteProjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convidado_id", nullable = false)
    private Usuario convidado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convidante_id", nullable = false)
    private Usuario convidante;

    @Column(length = 100)
    private String funcao;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusConvite status = StatusConvite.PENDENTE;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;
}
