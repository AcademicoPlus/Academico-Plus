package com.unifapi.academico.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unifapi.academico.domain.models.UsuarioHabilidade;

@Repository
public interface UsuarioHabilidadeRepository extends JpaRepository<UsuarioHabilidade, UUID> {

    @EntityGraph(attributePaths = {"habilidade"})
    List<UsuarioHabilidade> findByUsuario_IdOrderByHabilidade_NomeAsc(UUID usuarioId);

    @EntityGraph(attributePaths = {"habilidade", "usuario"})
    Optional<UsuarioHabilidade> findByUsuario_IdAndHabilidade_Id(UUID usuarioId, UUID habilidadeId);

    /** Alimenta Habilidade.usuariosCount. */
    long countByHabilidade_Id(UUID habilidadeId);

    long countByUsuario_Id(UUID usuarioId);

    boolean existsByUsuario_IdAndHabilidade_Id(UUID usuarioId, UUID habilidadeId);
}
