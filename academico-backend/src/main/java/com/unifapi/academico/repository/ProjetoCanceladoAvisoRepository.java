package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.ProjetoCanceladoAviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjetoCanceladoAvisoRepository extends JpaRepository<ProjetoCanceladoAviso, UUID> {

    /**
     * Busca avisos de cancelamento/modificação de projeto não visualizados pelo usuário,
     * ordenados do mais antigo para o mais recente.
     */
    List<ProjetoCanceladoAviso> findByUsuario_IdAndVisualizadoEmIsNullOrderByCriadoEmAsc(UUID usuarioId);
}