package com.unifapi.academico.repository;

import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.models.DenunciaAvaliacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DenunciaAvaliacaoRepository extends JpaRepository<DenunciaAvaliacao, UUID> {

    /** Só relações ManyToOne — seguro com paginação. */
    @EntityGraph(attributePaths = {
            "avaliacao", "avaliacao.avaliador", "avaliacao.avaliado", "denunciante"
    })
    Page<DenunciaAvaliacao> findByStatusOrderByCriadoEmAsc(StatusDenuncia status, Pageable pageable);

    boolean existsByAvaliacao_IdAndDenunciante_Id(UUID avaliacaoId, UUID denuncianteId);
}
