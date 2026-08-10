package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.Avaliacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {

    /** Só junta relações ManyToOne (avaliador, projeto) — seguro com paginação. */
    @EntityGraph(attributePaths = {"avaliador", "avaliador.curso", "avaliador.nivelAcesso", "projeto"})
    Page<Avaliacao> findByAvaliado_IdAndRemovidaFalseOrderByCriadoEmDesc(UUID avaliadoId, Pageable pageable);

    boolean existsByProjeto_IdAndAvaliador_IdAndAvaliado_IdAndRemovidaFalse(
            UUID projetoId, UUID avaliadorId, UUID avaliadoId);

    long countByAvaliado_IdAndRemovidaFalse(UUID avaliadoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.avaliado.id = :avaliadoId AND a.removida = false")
    Double calcularMediaPorAvaliado(@Param("avaliadoId") UUID avaliadoId);
}
