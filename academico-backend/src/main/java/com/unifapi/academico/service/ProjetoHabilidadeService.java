package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.domain.models.ProjetoHabilidade;
import com.unifapi.academico.dto.habilidade.ProjetoHabilidadeResponse;
import com.unifapi.academico.dto.habilidade.VincularHabilidadeRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.ProjetoHabilidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Habilidades necessárias de um projeto. É a tabela de junção entre Projeto
 * e o catálogo de Habilidade — não cria habilidades novas, apenas vincula
 * as existentes (ver HabilidadeService para o CRUD do catálogo).
 */
@Service
@RequiredArgsConstructor
public class ProjetoHabilidadeService {

    private final ProjetoHabilidadeRepository projetoHabilidadeRepository;
    private final ProjetoService projetoService;
    private final HabilidadeService habilidadeService;

    // ============================================
    // LEITURA
    // ============================================

    @Transactional(readOnly = true)
    public List<ProjetoHabilidadeResponse> listarPorProjeto(UUID projetoId) {
        projetoService.obterEntidadeAtiva(projetoId);

        return projetoHabilidadeRepository.findByProjeto_IdOrderByHabilidade_NomeAsc(projetoId).stream()
                .map(ProjetoHabilidadeResponse::fromEntity)
                .toList();
    }

    // ============================================
    // ESCRITA
    // ============================================

    @Transactional
    public ProjetoHabilidadeResponse vincular(UUID projetoId,
                                              VincularHabilidadeRequest request,
                                              UUID usuarioLogadoId) {
        var projeto = projetoService.obterEntidadeComoDono(projetoId, usuarioLogadoId);
        Habilidade habilidade = habilidadeService.obterEntidade(request.habilidadeId());

        if (projetoHabilidadeRepository.existsByProjeto_IdAndHabilidade_Id(projetoId, habilidade.getId())) {
            throw new RegraDeNegocioException("A habilidade '" + habilidade.getNome()
                    + "' já está vinculada a este projeto.");
        }

        ProjetoHabilidade vinculo = projetoHabilidadeRepository.save(ProjetoHabilidade.builder()
                .projeto(projeto)
                .habilidade(habilidade)
                .obrigatoria(Boolean.TRUE.equals(request.obrigatoria()))
                .build());

        projetoHabilidadeRepository.flush();
        habilidadeService.recalcularContadores(habilidade.getId());

        return ProjetoHabilidadeResponse.fromEntity(vinculo);
    }

    @Transactional
    public ProjetoHabilidadeResponse alterarObrigatoriedade(UUID projetoId,
                                                            UUID habilidadeId,
                                                            boolean obrigatoria,
                                                            UUID usuarioLogadoId) {
        projetoService.obterEntidadeComoDono(projetoId, usuarioLogadoId);

        ProjetoHabilidade vinculo = obterVinculo(projetoId, habilidadeId);
        vinculo.setObrigatoria(obrigatoria);

        return ProjetoHabilidadeResponse.fromEntity(projetoHabilidadeRepository.save(vinculo));
    }

    @Transactional
    public void desvincular(UUID projetoId, UUID habilidadeId, UUID usuarioLogadoId) {
        projetoService.obterEntidadeComoDono(projetoId, usuarioLogadoId);

        ProjetoHabilidade vinculo = obterVinculo(projetoId, habilidadeId);

        projetoHabilidadeRepository.delete(vinculo);
        projetoHabilidadeRepository.flush();

        habilidadeService.recalcularContadores(habilidadeId);
    }

    // ============================================
    // PRIVADOS
    // ============================================

    private ProjetoHabilidade obterVinculo(UUID projetoId, UUID habilidadeId) {
        return projetoHabilidadeRepository.findByProjeto_IdAndHabilidade_Id(projetoId, habilidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Esta habilidade não está vinculada ao projeto informado."));
    }
}
