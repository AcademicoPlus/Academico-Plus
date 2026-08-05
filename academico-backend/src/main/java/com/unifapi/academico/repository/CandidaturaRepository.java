package com.unifapi.academico.repository;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.models.Candidatura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {

    // ============================================
    // CONSULTAS
    // ============================================

    @Override
    @EntityGraph(attributePaths = {"usuario", "usuario.curso", "usuario.nivelAcesso", "projeto"})
    Optional<Candidatura> findById(UUID id);

    /** Candidaturas recebidas por um projeto (visão do criador). */
    @EntityGraph(attributePaths = {"usuario", "usuario.curso", "usuario.nivelAcesso", "projeto"})
    Page<Candidatura> findByProjeto_Id(UUID projetoId, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "usuario.curso", "usuario.nivelAcesso", "projeto"})
    Page<Candidatura> findByProjeto_IdAndStatus(UUID projetoId, StatusCandidatura status, Pageable pageable);

    /** Candidaturas enviadas por um usuário (visão do candidato). */
    @EntityGraph(attributePaths = {"usuario", "usuario.curso", "usuario.nivelAcesso", "projeto"})
    Page<Candidatura> findByUsuario_Id(UUID usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "usuario.curso", "usuario.nivelAcesso", "projeto"})
    Page<Candidatura> findByUsuario_IdAndStatus(UUID usuarioId, StatusCandidatura status, Pageable pageable);

    // ============================================
    // VALIDAÇÕES E CONTAGENS
    // ============================================

    /** Respeita a unique constraint (projeto_id, usuario_id): impede candidatura duplicada. */
    boolean existsByProjeto_IdAndUsuario_Id(UUID projetoId, UUID usuarioId);

    long countByProjeto_IdAndStatus(UUID projetoId, StatusCandidatura status);

    long countByUsuario_IdAndStatus(UUID usuarioId, StatusCandidatura status);
}
