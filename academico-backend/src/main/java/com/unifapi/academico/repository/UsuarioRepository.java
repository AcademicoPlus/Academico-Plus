package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.Usuario;
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

    // Equivalente ao list_active_collaborators
    @EntityGraph(attributePaths = {"nivelAcesso", "curso"})
    List<Usuario> findByAtivoTrueAndDeletadoEmIsNullAndNivelAcessoNome(String nomePerfil);

    // ============================================
    // SPECIFICATIONS PARA FILTROS DINÂMICOS
    // ============================================

    static Specification<Usuario> comStatus(String status) {
        return (root, query, cb) -> {
            if ("ativos".equalsIgnoreCase(status)) {
                return cb.and(cb.isTrue(root.get("ativo")), cb.isNull(root.get("deletedAt")));
            } else if ("inativos".equalsIgnoreCase(status)) {
                return cb.and(cb.isFalse(root.get("ativo")), cb.isNull(root.get("deletedAt")));
            } else if ("excluidos".equalsIgnoreCase(status)) {
                return cb.isNotNull(root.get("deletedAt"));
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
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("nome")), pattern),
                cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }

}