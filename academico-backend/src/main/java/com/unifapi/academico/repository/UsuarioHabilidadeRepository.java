package com.unifapi.academico.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unifapi.academico.domain.models.UsuarioHabilidade;

@Repository
public interface UsuarioHabilidadeRepository extends JpaRepository<UsuarioHabilidade, UUID> {

    /** Alimenta Habilidade.usuariosCount. */
    long countByHabilidade_Id(UUID habilidadeId);

    long countByUsuario_Id(UUID usuarioId);

    boolean existsByUsuario_IdAndHabilidade_Id(UUID usuarioId, UUID habilidadeId);
}
