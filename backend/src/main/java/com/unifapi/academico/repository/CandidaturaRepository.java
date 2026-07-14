package com.unifapi.academico.repository;

import com.unifapi.academico.domain.model.Candidatura;
import com.unifapi.academico.domain.enums.StatusCandidatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidaturaRepository extends JpaRepository<Candidatura, Long> {
    Optional<Candidatura> findByProjetoIdAndUsuarioId(Long projetoId, Long usuarioId);
    List<Candidatura> findByProjetoId(Long projetoId);
    List<Candidatura> findByStatus(StatusCandidatura status);
}