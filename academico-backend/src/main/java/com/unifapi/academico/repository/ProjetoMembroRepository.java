package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.ProjetoMembro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjetoMembroRepository extends JpaRepository<ProjetoMembro, UUID> {

    // ============================================
    // CONSULTAS
    // ============================================

    @Override
    @EntityGraph(attributePaths = {"usuario", "usuario.curso", "usuario.nivelAcesso", "projeto"})
    Optional<ProjetoMembro> findById(UUID id);

    @EntityGraph(attributePaths = {"usuario", "usuario.curso", "usuario.nivelAcesso"})
    List<ProjetoMembro> findByProjeto_IdOrderByDataAdesaoAsc(UUID projetoId);

    @EntityGraph(attributePaths = {"usuario", "usuario.curso", "usuario.nivelAcesso"})
    Optional<ProjetoMembro> findByProjeto_IdAndUsuario_Id(UUID projetoId, UUID usuarioId);

    // ============================================
    // VALIDAÇÕES E CONTAGENS
    // ============================================

    /** Respeita a unique constraint (projeto_id, usuario_id). */
    boolean existsByProjeto_IdAndUsuario_Id(UUID projetoId, UUID usuarioId);

    /** Fonte de verdade para recalcular Projeto.vagasPreenchidas. */
    long countByProjeto_Id(UUID projetoId);

    long countByUsuario_Id(UUID usuarioId);
}
