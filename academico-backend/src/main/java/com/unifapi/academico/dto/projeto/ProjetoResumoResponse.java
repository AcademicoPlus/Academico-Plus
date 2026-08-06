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
 * Payload de listagem, moldado para o card de projeto do frontend.
 * Não toca nas coleções LAZY do projeto (membros, candidaturas), o que evita
 * N+1 ao paginar — habilidadesNecessarias é a exceção, carregada via
 * ProjetoRepository.buscarAtivos com EntityGraph.
 */
public record ProjetoResumoResponse(
        UUID id,
        UsuarioResumoResponse criador,
        String titulo,
        String descricao,
        StatusProjeto status,
        List<ProjetoHabilidadeResponse> habilidadesNecessarias,
        Integer vagas,
        Integer vagasPreenchidas,
        LocalDateTime dataFim
) {
    public static ProjetoResumoResponse fromEntity(Projeto projeto) {
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
                habilidades,
                projeto.getVagas(),
                projeto.getVagasPreenchidas(),
                projeto.getDataFim()
        );
    }
}
