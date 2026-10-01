package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.LeituraChatProjeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeituraChatProjetoRepository extends JpaRepository<LeituraChatProjeto, UUID> {

    Optional<LeituraChatProjeto> findByProjeto_IdAndUsuario_Id(UUID projetoId, UUID usuarioId);
}
