package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.MensagemProjeto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensagemProjetoRepository extends JpaRepository<MensagemProjeto, UUID> {

    @EntityGraph(attributePaths = {"autor", "autor.curso", "autor.nivelAcesso"})
    List<MensagemProjeto> findByProjeto_IdOrderByCriadoEmAsc(UUID projetoId);
}
