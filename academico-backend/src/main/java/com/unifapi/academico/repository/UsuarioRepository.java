package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.Usuario;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {

    @EntityGraph(attributePaths = {"nivelAcesso", "curso"})
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM Usuario u JOIN FETCH u.nivelAcesso WHERE u.id = :id")
    Optional<Usuario> findByIdWithNivelAcesso(@Param("id") UUID id);

    List<Usuario> findByAtivoTrueAndDeletadoEmIsNullAndNivelAcessoNome(String nomePerfil);

    /**
     * Carrega todos os usuários ativos com as habilidades já inicializadas —
     * usada pelo RecomendacaoService para ranquear candidatos a um projeto.
     * @SQLRestriction("ativo = true") na entidade já filtra os inativos.
     */
    @EntityGraph(attributePaths = {"habilidades", "habilidades.habilidade"})
    @Query("SELECT u FROM Usuario u")
    List<Usuario> findTodosComHabilidades();

    // ============================================
    // SPECIFICATIONS PARA FILTROS DINÂMICOS
    // ============================================

    static Specification<Usuario> comFetchRelacoes() {
        return (root, query, cb) -> {
            // Evita exceção ao contar total de elementos na paginação (count query)
            if (Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType())) {
                return cb.conjunction();
            }
            root.fetch("nivelAcesso", JoinType.LEFT);
            root.fetch("curso", JoinType.LEFT);
            return cb.conjunction();
        };
    }

    static Specification<Usuario> somenteAtivos() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("ativo")),
                cb.isNull(root.get("deletadoEm"))
        );
    }

    static Specification<Usuario> semAdmin() {
        return (root, query, cb) -> cb.notEqual(
                cb.upper(root.join("nivelAcesso").get("nome")), "ADMIN"
        );
    }

    static Specification<Usuario> comCurso(UUID idCurso) {
        return (root, query, cb) -> {
            if (idCurso == null) return cb.conjunction();
            return cb.equal(root.get("curso").get("id"), idCurso);
        };
    }

    static Specification<Usuario> comStatus(String status) {
        return (root, query, cb) -> {
            if ("ativos".equalsIgnoreCase(status)) {
                return cb.and(cb.isTrue(root.get("ativo")), cb.isNull(root.get("deletadoEm")));
            } else if ("inativos".equalsIgnoreCase(status)) {
                return cb.and(cb.isFalse(root.get("ativo")), cb.isNull(root.get("deletadoEm")));
            } else if ("excluidos".equalsIgnoreCase(status)) {
                return cb.isNotNull(root.get("deletadoEm"));
            }
            return cb.conjunction();
        };
    }

    static Specification<Usuario> comPerfil(String permission) {
        return (root, query, cb) -> {
            if (permission == null || permission.isBlank()) return cb.conjunction();
            return cb.equal(root.join("nivelAcesso").get("nome"), permission);
        };
    }

    static Specification<Usuario> comBuscaTextual(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("nome")), pattern),
                cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }
}