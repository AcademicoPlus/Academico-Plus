package com.unifapi.academico.dto.projeto;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.habilidade.ProjetoHabilidadeResponse;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Payload completo (detalhe do projeto). As habilidades são lidas da entidade,
 * portanto deve ser construído dentro da transação e a partir de um projeto
 * carregado com a coleção inicializada (ver ProjetoRepository.findDetalhePorId).
 */
public record ProjetoResponse(
        UUID id,
        UsuarioResumoResponse criador,
        String titulo,
        String descricao,
        StatusProjeto status,
        Integer vagas,
        Integer vagasPreenchidas,
        int vagasDisponiveis,
        boolean aceitandoCandidaturas,
        LocalDateTime dataFim,
        Boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        List<ProjetoHabilidadeResponse> habilidadesNecessarias,
        long totalMembros,
        long totalCandidaturasPendentes
) {
    public static ProjetoResponse fromEntity(Projeto projeto,
                                             long totalMembros,
                                             long totalCandidaturasPendentes) {
        int vagas = projeto.getVagas() != null ? projeto.getVagas() : 0;
        int preenchidas = projeto.getVagasPreenchidas() != null ? projeto.getVagasPreenchidas() : 0;

        List<ProjetoHabilidadeResponse> habilidades = projeto.getHabilidadesNecessarias() == null
                ? List.of()
                : projeto.getHabilidadesNecessarias().stream()
                        .map(ProjetoHabilidadeResponse::fromEntity)
                        // Set não tem ordem definida: ordena para deixar o payload estável
                        .sorted(Comparator.comparing(
                                h -> h.habilidade() != null ? h.habilidade().nome() : "",
                                String.CASE_INSENSITIVE_ORDER))
                        .toList();

        return new ProjetoResponse(
                projeto.getId(),
                projeto.getCriador() != null ? UsuarioResumoResponse.fromEntity(projeto.getCriador()) : null,
                projeto.getTitulo(),
                projeto.getDescricao(),
                projeto.getStatus(),
                projeto.getVagas(),
                projeto.getVagasPreenchidas(),
                Math.max(0, vagas - preenchidas),
                projeto.podeAceitarCandidato(),
                projeto.getDataFim(),
                projeto.getAtivo(),
                projeto.getCriadoEm(),
                projeto.getAtualizadoEm(),
                habilidades,
                totalMembros,
                totalCandidaturasPendentes
        );
    }
}
