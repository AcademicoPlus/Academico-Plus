package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.ProjetoHabilidade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjetoHabilidadeRepository extends JpaRepository<ProjetoHabilidade, UUID> {

    // ============================================
    // CONSULTAS
    // ============================================

    @EntityGraph(attributePaths = {"habilidade"})
    List<ProjetoHabilidade> findByProjeto_IdOrderByHabilidade_NomeAsc(UUID projetoId);

    @EntityGraph(attributePaths = {"habilidade", "projeto"})
    Optional<ProjetoHabilidade> findByProjeto_IdAndHabilidade_Id(UUID projetoId, UUID habilidadeId);

    // ============================================
    // VALIDAÇÕES E CONTAGENS
    // ============================================

    /** Respeita a unique constraint (projeto_id, habilidade_id). */
    boolean existsByProjeto_IdAndHabilidade_Id(UUID projetoId, UUID habilidadeId);

    /** Alimenta Habilidade.projetosCount. */
    long countByHabilidade_Id(UUID habilidadeId);

    long countByProjeto_Id(UUID projetoId);
}
