package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.Habilidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabilidadeRepository extends JpaRepository<Habilidade, UUID> {

    // ============================================
    // CONSULTAS
    // ============================================

    @Query(value = """
            SELECT h FROM Habilidade h
            WHERE (:busca IS NULL OR LOWER(h.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
              AND (:categoria IS NULL OR LOWER(h.categoria) = LOWER(CAST(:categoria AS string)))
            """,
            countQuery = """
            SELECT COUNT(h) FROM Habilidade h
            WHERE (:busca IS NULL OR LOWER(h.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
              AND (:categoria IS NULL OR LOWER(h.categoria) = LOWER(CAST(:categoria AS string)))
            """)
    Page<Habilidade> buscar(@Param("busca") String busca,
                            @Param("categoria") String categoria,
                            Pageable pageable);

    @Query("SELECT DISTINCT h.categoria FROM Habilidade h WHERE h.categoria IS NOT NULL ORDER BY h.categoria ASC")
    List<String> listarCategorias();

    // ============================================
    // VALIDAÇÕES (nome é unique na tabela)
    // ============================================

    Optional<Habilidade> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    /** Usado no update, para não acusar conflito com a própria habilidade. */
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);
}
