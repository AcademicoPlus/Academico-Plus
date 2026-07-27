package com.unifapi.academico.repository;

import com.unifapi.academico.domain.model.Candidatura;
import com.unifapi.academico.domain.enums.StatusCandidatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {
    Optional<Candidatura> findByProjetoIdAndUsuarioId(UUID projetoId, UUID usuarioId);
    List<Candidatura> findByProjetoId(UUID projetoId);
    List<Candidatura> findByStatus(StatusCandidatura status);
}