package com.unifapi.academico.repository;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Projeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {

    /**
     * Carrega todos os projetos elegíveis para recomendação, já com as
     * habilidades necessárias — usada pelo RecomendacaoService, que percorre
     * a lista inteira em memória (sem paginação, então o fetch join da
     * coleção não tem o problema de LIMIT/OFFSET das listagens paginadas).
     */
    @EntityGraph(attributePaths = {
            "criador", "criador.curso", "criador.nivelAcesso",
            "habilidadesNecessarias", "habilidadesNecessarias.habilidade"
    })
    List<Projeto> findByAtivoTrueAndStatus(StatusProjeto status);

    // ============================================
    // LISTAGENS
    // O EntityGraph traz o criador (e curso/nivelAcesso) na mesma consulta,
    // evitando N+1 ao montar o UsuarioResumoResponse de cada item da página.
    // As coleções NÃO são carregadas aqui — listagem usa ProjetoResumoResponse.
    // ============================================

    @EntityGraph(attributePaths = {"criador", "criador.curso", "criador.nivelAcesso"})
    @Query(value = """
            SELECT p FROM Projeto p
            WHERE p.ativo = true
              AND (:status IS NULL OR p.status = :status)
              AND (:busca IS NULL
                   OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(p.descricao) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(p) FROM Projeto p
            WHERE p.ativo = true
              AND (:status IS NULL OR p.status = :status)
              AND (:busca IS NULL
                   OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(p.descricao) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    Page<Projeto> buscarAtivos(@Param("status") StatusProjeto status,
                               @Param("busca") String busca,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"criador", "criador.curso", "criador.nivelAcesso"})
    Page<Projeto> findByCriador_IdAndAtivoTrue(UUID criadorId, Pageable pageable);

    /**
     * Projetos ativos em que o usuário está envolvido: criados por ele OU em
     * que é membro (dashboard/menu "Meus Projetos"). Duas variantes em vez de
     * ":status IS NULL OR ..." para não depender da inferência de tipo do
     * parâmetro nulo no Postgres.
     */
    @EntityGraph(attributePaths = {"criador", "criador.curso", "criador.nivelAcesso"})
    @Query("""
            SELECT p FROM Projeto p
            WHERE p.ativo = true
              AND (p.criador.id = :usuarioId
                   OR EXISTS (SELECT 1 FROM ProjetoMembro m WHERE m.projeto = p AND m.usuario.id = :usuarioId))
            """)
    Page<Projeto> buscarVinculados(@Param("usuarioId") UUID usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"criador", "criador.curso", "criador.nivelAcesso"})
    @Query("""
            SELECT p FROM Projeto p
            WHERE p.ativo = true
              AND p.status = :status
              AND (p.criador.id = :usuarioId
                   OR EXISTS (SELECT 1 FROM ProjetoMembro m WHERE m.projeto = p AND m.usuario.id = :usuarioId))
            """)
    Page<Projeto> buscarVinculadosPorStatus(@Param("usuarioId") UUID usuarioId,
                                            @Param("status") StatusProjeto status,
                                            Pageable pageable);

    /** Mesma regra de buscarVinculados, sem paginação — as conversas do usuário. */
    @Query("""
            SELECT p FROM Projeto p
            WHERE p.ativo = true
              AND (p.criador.id = :usuarioId
                   OR EXISTS (SELECT 1 FROM ProjetoMembro m WHERE m.projeto = p AND m.usuario.id = :usuarioId))
            """)
    List<Projeto> listarVinculados(@Param("usuarioId") UUID usuarioId);

    /** Projetos em que o usuário participa como membro (não como criador). */
    @EntityGraph(attributePaths = {"criador", "criador.curso", "criador.nivelAcesso"})
    Page<Projeto> findByMembros_Usuario_IdAndAtivoTrue(UUID usuarioId, Pageable pageable);

    // ============================================
    // DETALHE
    // ============================================

    /** Carrega o projeto já com criador e habilidades necessárias para o response completo. */
    @EntityGraph(attributePaths = {
            "criador", "criador.curso", "criador.nivelAcesso",
            "habilidadesNecessarias", "habilidadesNecessarias.habilidade"
    })
    @Query("SELECT p FROM Projeto p WHERE p.id = :id AND p.ativo = true")
    Optional<Projeto> findDetalhePorId(@Param("id") UUID id);

    /** Busca enxuta, usada nas validações de escrita (não carrega coleções). */
    Optional<Projeto> findByIdAndAtivoTrue(UUID id);

    long countByCriador_IdAndAtivoTrue(UUID criadorId);
}
