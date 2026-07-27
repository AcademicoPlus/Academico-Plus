package com.unifapi.academico.repository;

import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.enums.StatusProjeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {

    List<Projeto> findByCriadorId(UUID criadorId);

    @EntityGraph(attributePaths = {"criador", "habilidadesNecessarias.habilidade"})
    Page<Projeto> findByStatus(StatusProjeto status, Pageable pageable);

    @EntityGraph(attributePaths = {"criador", "habilidadesNecessarias.habilidade"})
    Page<Projeto> findByAtivoTrue(Pageable pageable);
}