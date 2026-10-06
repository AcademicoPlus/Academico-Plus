package com.unifapi.academico.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tokens_revogados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevogado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 1000, nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime dataRevogacao;

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    public TokenRevogado(String token, LocalDateTime dataRevogacao, LocalDateTime dataExpiracao) {
        this.token = token;
        this.dataRevogacao = dataRevogacao;
        this.dataExpiracao = dataExpiracao;
    }
}