package com.unifapi.academico.repository;

import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.enums.TipoAlvoDenuncia;
import com.unifapi.academico.domain.models.Denuncia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DenunciaRepository extends JpaRepository<Denuncia, UUID> {

    /**
     * Só relações ManyToOne no grafo — seguro com paginação (do contrário o
     * Hibernate pagina em memória).
     */
    @EntityGraph(attributePaths = {
            "avaliacao", "avaliacao.avaliador", "avaliacao.avaliado",
            "projeto", "projeto.criador",
            "usuarioDenunciado", "denunciante"
    })
    Page<Denuncia> findByStatusOrderByCriadoEmAsc(StatusDenuncia status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "avaliacao", "avaliacao.avaliador", "avaliacao.avaliado",
            "projeto", "projeto.criador",
            "usuarioDenunciado", "denunciante"
    })
    Page<Denuncia> findByStatusAndTipoAlvoOrderByCriadoEmAsc(
            StatusDenuncia status, TipoAlvoDenuncia tipoAlvo, Pageable pageable);

    boolean existsByAvaliacao_IdAndDenunciante_Id(UUID avaliacaoId, UUID denuncianteId);

    boolean existsByProjeto_IdAndDenunciante_Id(UUID projetoId, UUID denuncianteId);

    boolean existsByUsuarioDenunciado_IdAndDenunciante_Id(UUID usuarioDenunciadoId, UUID denuncianteId);
}
