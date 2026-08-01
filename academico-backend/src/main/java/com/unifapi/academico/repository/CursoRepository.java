package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CursoRepository extends JpaRepository<Curso, UUID> {
    
    Optional<Curso> findById(UUID idCurso);

    Optional<Curso> findByNomeIgnoreCase(String nomeCurso);
}