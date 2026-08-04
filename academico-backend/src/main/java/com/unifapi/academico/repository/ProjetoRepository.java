package com.unifapi.academico.repository;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Projeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {

    Page<Projeto> findByAtivoTrue(Pageable pageable);

    Page<Projeto> findByAtivoTrueAndStatus(StatusProjeto status, Pageable pageable);

    Page<Projeto> findByCriador_IdAndAtivoTrue(UUID criadorId, Pageable pageable);

    Optional<Projeto> findByIdAndAtivoTrue(UUID id);
}