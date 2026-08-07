package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.ProjetoHabilidade;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.habilidade.HabilidadeResumoResponse;
import com.unifapi.academico.dto.projeto.ProjetoResumoResponse;
import com.unifapi.academico.dto.recomendacao.ProjetoRecomendadoResponse;
import com.unifapi.academico.dto.recomendacao.UsuarioRecomendadoResponse;
import com.unifapi.academico.repository.CandidaturaRepository;
import com.unifapi.academico.repository.ProjetoHabilidadeRepository;
import com.unifapi.academico.repository.ProjetoMembroRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioHabilidadeRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Recomendação por compatibilidade de habilidades, inspirada no Índice de
 * Jaccard mas adaptada para medir COBERTURA dos requisitos do projeto em vez
 * de similaridade simétrica: o Jaccard puro (|interseção|/|união|) penaliza
 * quem tem muitas habilidades fora do que o projeto pede, o que não faz
 * sentido aqui — ter uma habilidade extra não deveria reduzir a pontuação
 * de compatibilidade de alguém que já atende tudo que é exigido.
 *
 * Fórmula: soma dos pesos das habilidades exigidas que a outra parte possui,
 * dividida pela soma dos pesos de todas as habilidades exigidas do projeto.
 * Habilidades obrigatórias pesam o dobro das desejáveis.
 */
@Service
@RequiredArgsConstructor
public class RecomendacaoService {

    private static final int PESO_OBRIGATORIA = 2;
    private static final int PESO_DESEJAVEL = 1;
    private static final int LIMITE_PADRAO = 10;
    private static final int LIMITE_MAXIMO = 50;

    private final ProjetoService projetoService;
    private final ProjetoRepository projetoRepository;
    private final ProjetoHabilidadeRepository projetoHabilidadeRepository;
    private final ProjetoMembroRepository projetoMembroRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final UsuarioHabilidadeRepository usuarioHabilidadeRepository;
    private final UsuarioRepository usuarioRepository;

    // ============================================
    // PROJETOS RECOMENDADOS PARA UM USUÁRIO
    // ============================================

    @Transactional(readOnly = true)
    public List<ProjetoRecomendadoResponse> recomendarProjetos(UUID usuarioId, Integer limite) {
        Set<UUID> habilidadesUsuario = usuarioHabilidadeRepository
                .findByUsuario_IdOrderByHabilidade_NomeAsc(usuarioId).stream()
                .map(uh -> uh.getHabilidade().getId())
                .collect(Collectors.toSet());

        if (habilidadesUsuario.isEmpty()) {
            return List.of();
        }

        List<Projeto> candidatos = projetoRepository.findByAtivoTrueAndStatus(StatusProjeto.ABERTO);

        return candidatos.stream()
                .filter(p -> p.getCriador() == null || !p.getCriador().getId().equals(usuarioId))
                .filter(p -> !projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(p.getId(), usuarioId))
                .filter(p -> !candidaturaRepository.existsByProjeto_IdAndUsuario_Id(p.getId(), usuarioId))
                .map(p -> paraProjetoRecomendado(p, habilidadesUsuario))
                .filter(r -> r != null && r.compatibilidade() > 0)
                .sorted(Comparator.comparingDouble(ProjetoRecomendadoResponse::compatibilidade).reversed())
                .limit(limitar(limite))
                .toList();
    }

    // ============================================
    // CANDIDATOS RECOMENDADOS PARA UM PROJETO
    // ============================================

    @Transactional(readOnly = true)
    public List<UsuarioRecomendadoResponse> recomendarCandidatos(UUID projetoId, UUID usuarioLogadoId, Integer limite) {
        // Só o criador enxerga recomendação de candidatos do próprio projeto.
        projetoService.obterEntidadeComoDono(projetoId, usuarioLogadoId);

        List<ProjetoHabilidade> requisitos = projetoHabilidadeRepository
                .findByProjeto_IdOrderByHabilidade_NomeAsc(projetoId);

        if (requisitos.isEmpty()) {
            return List.of();
        }

        List<Usuario> candidatos = usuarioRepository.findTodosComHabilidades();

        return candidatos.stream()
                .filter(u -> !u.getId().equals(usuarioLogadoId))
                .filter(u -> !projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projetoId, u.getId()))
                .filter(u -> !candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projetoId, u.getId()))
                .map(u -> paraUsuarioRecomendado(u, requisitos))
                .filter(r -> r != null && r.compatibilidade() > 0)
                .sorted(Comparator.comparingDouble(UsuarioRecomendadoResponse::compatibilidade).reversed())
                .limit(limitar(limite))
                .toList();
    }

    // ============================================
    // PRIVADOS
    // ============================================

    private ProjetoRecomendadoResponse paraProjetoRecomendado(Projeto projeto, Set<UUID> habilidadesUsuario) {
        List<ProjetoHabilidade> requisitos = projeto.getHabilidadesNecessarias() == null
                ? List.of()
                : List.copyOf(projeto.getHabilidadesNecessarias());

        Resultado resultado = calcularCompatibilidade(requisitos, habilidadesUsuario);
        if (resultado == null) {
            return null;
        }

        return new ProjetoRecomendadoResponse(
                ProjetoResumoResponse.fromEntity(projeto),
                resultado.score(),
                resultado.habilidadesEmComum()
        );
    }

    private UsuarioRecomendadoResponse paraUsuarioRecomendado(Usuario usuario, List<ProjetoHabilidade> requisitos) {
        Set<UUID> habilidadesUsuario = usuario.getHabilidades() == null
                ? Set.of()
                : usuario.getHabilidades().stream()
                        .map(uh -> uh.getHabilidade().getId())
                        .collect(Collectors.toSet());

        Resultado resultado = calcularCompatibilidade(requisitos, habilidadesUsuario);
        if (resultado == null) {
            return null;
        }

        return new UsuarioRecomendadoResponse(
                UsuarioResumoResponse.fromEntity(usuario),
                resultado.score(),
                resultado.habilidadesEmComum()
        );
    }

    /** Retorna null quando o projeto não tem nenhum requisito cadastrado (nada a comparar). */
    private Resultado calcularCompatibilidade(List<ProjetoHabilidade> requisitos, Set<UUID> habilidadesDaOutraParte) {
        if (requisitos.isEmpty()) {
            return null;
        }

        int pesoTotal = 0;
        int pesoAtendido = 0;
        List<HabilidadeResumoResponse> emComum = new ArrayList<>();

        for (ProjetoHabilidade requisito : requisitos) {
            Habilidade habilidade = requisito.getHabilidade();
            int peso = Boolean.TRUE.equals(requisito.getObrigatoria()) ? PESO_OBRIGATORIA : PESO_DESEJAVEL;
            pesoTotal += peso;

            if (habilidade != null && habilidadesDaOutraParte.contains(habilidade.getId())) {
                pesoAtendido += peso;
                emComum.add(HabilidadeResumoResponse.fromEntity(habilidade));
            }
        }

        double score = pesoTotal == 0 ? 0.0 : (double) pesoAtendido / pesoTotal;
        return new Resultado(score, emComum);
    }

    private int limitar(Integer limite) {
        if (limite == null || limite < 1) {
            return LIMITE_PADRAO;
        }
        return Math.min(limite, LIMITE_MAXIMO);
    }

    private record Resultado(double score, List<HabilidadeResumoResponse> habilidadesEmComum) {
    }
}
