package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.MensagemProjeto;
import com.unifapi.academico.dto.mensagem.NaoLidasPorProjeto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MensagemProjetoRepository extends JpaRepository<MensagemProjeto, UUID> {

    @EntityGraph(attributePaths = {"autor", "autor.curso", "autor.nivelAcesso"})
    List<MensagemProjeto> findByProjeto_IdOrderByCriadoEmAsc(UUID projetoId);

    /**
     * Última mensagem de cada projeto informado (prévia da caixa "Mensagens").
     * Empates de criado_em podem trazer mais de uma linha por projeto — o
     * service fica com uma só.
     */
    @EntityGraph(attributePaths = {"autor", "autor.curso", "autor.nivelAcesso", "projeto"})
    @Query("""
            SELECT m FROM MensagemProjeto m
            WHERE m.projeto.id IN :projetoIds
              AND m.criadoEm = (SELECT MAX(m2.criadoEm) FROM MensagemProjeto m2 WHERE m2.projeto = m.projeto)
            """)
    List<MensagemProjeto> buscarUltimasMensagens(@Param("projetoIds") Collection<UUID> projetoIds);

    /**
     * Mensagens de outros autores ainda não lidas pelo usuário, por projeto.
     * Sem registro de leitura, todas as mensagens alheias contam.
     */
    @Query("""
            SELECT new com.unifapi.academico.dto.mensagem.NaoLidasPorProjeto(m.projeto.id, COUNT(m))
            FROM MensagemProjeto m
            LEFT JOIN LeituraChatProjeto l ON l.projeto.id = m.projeto.id AND l.usuario.id = :usuarioId
            WHERE m.projeto.id IN :projetoIds
              AND m.autor.id <> :usuarioId
              AND (l.id IS NULL OR m.criadoEm > l.lidoAte)
            GROUP BY m.projeto.id
            """)
    List<NaoLidasPorProjeto> contarNaoLidas(@Param("usuarioId") UUID usuarioId,
                                            @Param("projetoIds") Collection<UUID> projetoIds);
}
