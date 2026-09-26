package com.unifapi.academico.repository;

import com.unifapi.academico.domain.enums.StatusConvite;
import com.unifapi.academico.domain.models.ConviteProjeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConviteProjetoRepository extends JpaRepository<ConviteProjeto, UUID> {

    // ============================================
    // CONSULTAS
    // ============================================

    @Override
    @EntityGraph(attributePaths = {"projeto", "convidado", "convidado.curso", "convidado.nivelAcesso",
            "convidante", "convidante.curso", "convidante.nivelAcesso"})
    Optional<ConviteProjeto> findById(UUID id);

    /** Convites enviados por um projeto (visão do criador). */
    @EntityGraph(attributePaths = {"convidado", "convidado.curso", "convidado.nivelAcesso", "projeto"})
    Page<ConviteProjeto> findByProjeto_Id(UUID projetoId, Pageable pageable);

    @EntityGraph(attributePaths = {"convidado", "convidado.curso", "convidado.nivelAcesso", "projeto"})
    Page<ConviteProjeto> findByProjeto_IdAndStatus(UUID projetoId, StatusConvite status, Pageable pageable);

    /** Convites recebidos pelo usuário, ignorando projetos inativados. */
    @EntityGraph(attributePaths = {"projeto", "convidante", "convidante.curso", "convidante.nivelAcesso"})
    Page<ConviteProjeto> findByConvidado_IdAndProjeto_AtivoTrue(UUID convidadoId, Pageable pageable);

    @EntityGraph(attributePaths = {"projeto", "convidante", "convidante.curso", "convidante.nivelAcesso"})
    Page<ConviteProjeto> findByConvidado_IdAndStatusAndProjeto_AtivoTrue(UUID convidadoId,
                                                                        StatusConvite status,
                                                                        Pageable pageable);

    // ============================================
    // VALIDAÇÕES E CONTAGENS
    // ============================================

    /** Espelha o índice único parcial: um único convite pendente por (projeto, convidado). */
    boolean existsByProjeto_IdAndConvidado_IdAndStatus(UUID projetoId, UUID convidadoId, StatusConvite status);

    /** Alimenta o badge de notificações do header. */
    long countByConvidado_IdAndStatusAndProjeto_AtivoTrue(UUID convidadoId, StatusConvite status);
}
