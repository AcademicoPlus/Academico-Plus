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
 * Payload de listagem. Não toca nas coleções LAZY do projeto (membros,
 * candidaturas, habilidades), o que evita N+1 ao paginar.
 */
public record ProjetoResumoResponse(
        UUID id,
        UsuarioResumoResponse criador,
        String titulo,
        String descricao,
        StatusProjeto status,
        Integer vagas,
        Integer vagasPreenchidas,
        int vagasDisponiveis,
        boolean aceitandoCandidaturas,
        List<ProjetoHabilidadeResponse> habilidadesNecessarias,
        LocalDateTime dataFim,
        LocalDateTime criadoEm
) {
    public static ProjetoResumoResponse fromEntity(Projeto projeto) {
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

        return new ProjetoResumoResponse(
                projeto.getId(),
                projeto.getCriador() != null ? UsuarioResumoResponse.fromEntity(projeto.getCriador()) : null,
                projeto.getTitulo(),
                projeto.getDescricao(),
                projeto.getStatus(),
                projeto.getVagas(),
                projeto.getVagasPreenchidas(),
                Math.max(0, vagas - preenchidas),
                projeto.podeAceitarCandidato(),
                habilidades,
                projeto.getDataFim(),
                projeto.getCriadoEm()
        );
    }
}
