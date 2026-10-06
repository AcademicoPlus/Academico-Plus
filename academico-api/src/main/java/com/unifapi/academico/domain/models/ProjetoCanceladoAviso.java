package com.unifapi.academico.domain.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projeto_cancelamento_aviso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoCanceladoAviso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_aviso", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projeto", nullable = false)
    private Projeto projeto;

    
    @Column(name = "projeto_nome", nullable = false, length = 200)
    private String projetoNome;

    @Column(name = "projeto_data", nullable = false)
    private LocalDate projetoData;

    @Builder.Default
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo = "cancelamento";

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    
    @Column(name = "visualizado_em")
    private LocalDateTime visualizadoEm;

    public UUID getProjetoId() {
        return projeto != null ? projeto.getId() : null;
    }

    public UUID getUsuarioId() {
        return usuario != null ? usuario.getId() : null;
    }
}