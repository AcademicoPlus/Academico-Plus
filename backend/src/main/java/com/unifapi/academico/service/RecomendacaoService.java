package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecomendacaoService {

    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Obter recomendações de colaboradores para um projeto
     * Usa Índice de Similaridade de Jaccard
     */
    public List<Map<String, Object>> obterRecomendacoes(Long projetoId, int limit) {
        Projeto projeto = projetoRepository.findById(projetoId)
            .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        // Habilidades necessárias do projeto
        Set<String> habilidadesProjetoDesprezadas = projeto.getHabilidadesNecessarias()
            .stream()
            .map(ph -> ph.getHabilidade().getNome())
            .collect(Collectors.toSet());

        // Se não há habilidades definidas, retornar lista vazia
        if (habilidadesProjetoDesprezadas.isEmpty()) {
            return Collections.emptyList();
        }

        // Buscar todos os usuários
        List<Usuario> usuarios = usuarioRepository.findAll();

        // Calcular similaridade de Jaccard para cada usuário
        List<Map<String, Object>> recomendacoes = usuarios.stream()
            .map(usuario -> {
                // Habilidades do usuário
                Set<String> habilidadesUsuario = usuario.getHabilidades()
                    .stream()
                    .map(uh -> uh.getHabilidade().getNome())
                    .collect(Collectors.toSet());

                // Calcular Jaccard Similarity
                double similaridade = calcularSimilaridade(
                    habilidadesProjetoDesprezadas,
                    habilidadesUsuario
                );

                // Retornar mapa com dados
                return Map.of(
                    "usuarioId", (Object) usuario.getId(),
                    "nome", usuario.getNome(),
                    "email", usuario.getEmail(),
                    "curso", usuario.getCurso(),
                    "similaridade", similaridade,
                    "habilidadesMatches", encontrarMatches(
                        habilidadesProjetoDesprezadas,
                        habilidadesUsuario
                    )
                );
            })
            // Filtrar apenas com similaridade > 0
            .filter(map -> (Double) map.get("similaridade") > 0)
            // Ordenar por similaridade DESC
            .sorted((a, b) -> Double.compare(
                (Double) b.get("similaridade"),
                (Double) a.get("similaridade")
            ))
            // Limitar resultado
            .limit(limit)
            .collect(Collectors.toList());

        return recomendacoes;
    }

    /**
     * Calcular índice de similaridade de Jaccard
     * J(A,B) = |A ∩ B| / |A ∪ B|
     */
    private double calcularSimilaridade(Set<String> conjuntoA, Set<String> conjuntoB) {
        if (conjuntoA.isEmpty() && conjuntoB.isEmpty()) {
            return 1.0;
        }

        // Interseção (A ∩ B)
        Set<String> intersecao = new HashSet<>(conjuntoA);
        intersecao.retainAll(conjuntoB);

        // União (A ∪ B)
        Set<String> uniao = new HashSet<>(conjuntoA);
        uniao.addAll(conjuntoB);

        // Evitar divisão por zero
        if (uniao.isEmpty()) {
            return 0.0;
        }

        // J = |intersecao| / |uniao|
        return (double) intersecao.size() / uniao.size();
    }

    /**
     * Encontrar habilidades que combinam
     */
    private List<String> encontrarMatches(Set<String> habilidadesA, Set<String> habilidadesB) {
        return habilidadesA.stream()
            .filter(habilidadesB::contains)
            .collect(Collectors.toList());
    }
}