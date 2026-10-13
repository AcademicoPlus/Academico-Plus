package com.unifapi.academico.repository;

import com.unifapi.academico.domain.enums.MedidaModeracao;
import com.unifapi.academico.domain.models.Sancao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SancaoRepository extends JpaRepository<Sancao, UUID> {

    /** Quantas sanções de um tipo o usuário já acumulou — base da sugestão de medida. */
    long countByUsuario_IdAndMedida(UUID usuarioId, MedidaModeracao medida);

    /** Sanções ainda não exibidas ao usuário; consumidas no login. */
    @EntityGraph(attributePaths = {"usuario"})
    List<Sancao> findByUsuario_IdAndVisualizadoEmIsNullOrderByCriadoEmAsc(UUID usuarioId);

    @EntityGraph(attributePaths = {"aplicadoPor"})
    List<Sancao> findByUsuario_IdOrderByCriadoEmDesc(UUID usuarioId);
}
