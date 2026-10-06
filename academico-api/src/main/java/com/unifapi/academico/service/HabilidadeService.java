package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.dto.habilidade.AtualizarHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.CriarHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.HabilidadeResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.HabilidadeRepository;
import com.unifapi.academico.repository.ProjetoHabilidadeRepository;
import com.unifapi.academico.repository.UsuarioHabilidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo de habilidades. É um recurso compartilhado entre projetos e usuários,
 * por isso a escrita é restrita a administradores (ver HabilidadeController).
 */
@Service
@RequiredArgsConstructor
public class HabilidadeService {

    private final HabilidadeRepository habilidadeRepository;
    private final ProjetoHabilidadeRepository projetoHabilidadeRepository;
    private final UsuarioHabilidadeRepository usuarioHabilidadeRepository;

    // ============================================
    // ESCRITA
    // ============================================

    @Transactional
    public HabilidadeResponse criar(CriarHabilidadeRequest request) {
        String nome = request.nome().trim();

        if (habilidadeRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraDeNegocioException("Já existe uma habilidade cadastrada com o nome '" + nome + "'.");
        }

        Habilidade habilidade = Habilidade.builder()
                .nome(nome)
                .categoria(normalizar(request.categoria()))
                .descricao(normalizar(request.descricao()))
                .build();

        return HabilidadeResponse.fromEntity(habilidadeRepository.save(habilidade));
    }

    @Transactional
    public HabilidadeResponse atualizar(UUID id, AtualizarHabilidadeRequest request) {
        Habilidade habilidade = obterEntidade(id);
        String nome = request.nome().trim();

        if (habilidadeRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraDeNegocioException("Já existe outra habilidade cadastrada com o nome '" + nome + "'.");
        }

        habilidade.setNome(nome);
        habilidade.setCategoria(normalizar(request.categoria()));
        habilidade.setDescricao(normalizar(request.descricao()));

        return HabilidadeResponse.fromEntity(habilidadeRepository.save(habilidade));
    }

    /**
     * Exclusão física: a tabela habilidades não tem coluna de soft delete.
     * Por isso só é permitida enquanto a habilidade não estiver em uso.
     */
    @Transactional
    public void remover(UUID id) {
        Habilidade habilidade = obterEntidade(id);

        long emProjetos = projetoHabilidadeRepository.countByHabilidade_Id(id);
        long emUsuarios = usuarioHabilidadeRepository.countByHabilidade_Id(id);

        if (emProjetos > 0 || emUsuarios > 0) {
            throw new RegraDeNegocioException(
                    "A habilidade '" + habilidade.getNome() + "' está em uso ("
                            + emProjetos + " projeto(s) e " + emUsuarios + " usuário(s)) e não pode ser removida.");
        }

        habilidadeRepository.delete(habilidade);
    }

    // ============================================
    // LEITURA
    // ============================================

    @Transactional(readOnly = true)
    public Page<HabilidadeResponse> listar(String busca, String categoria, Pageable pageable) {
        return habilidadeRepository.buscar(normalizar(busca), normalizar(categoria), pageable)
                .map(HabilidadeResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<String> listarCategorias() {
        return habilidadeRepository.listarCategorias();
    }

    @Transactional(readOnly = true)
    public HabilidadeResponse buscarPorId(UUID id) {
        return HabilidadeResponse.fromEntity(obterEntidade(id));
    }

    // ============================================
    // USO INTERNO (outros services)
    // ============================================

    public Habilidade obterEntidade(UUID id) {
        return habilidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Habilidade não encontrada com o ID informado."));
    }

    /**
     * Recalcula usuariosCount/projetosCount a partir das tabelas de vínculo.
     * É idempotente de propósito: chamado após vincular/desvincular, nunca
     * deixa os contadores divergirem do estado real.
     */
    @Transactional
    public void recalcularContadores(UUID habilidadeId) {
        habilidadeRepository.findById(habilidadeId).ifPresent(habilidade -> {
            habilidade.setProjetosCount((int) projetoHabilidadeRepository.countByHabilidade_Id(habilidadeId));
            habilidade.setUsuariosCount((int) usuarioHabilidadeRepository.countByHabilidade_Id(habilidadeId));
            habilidadeRepository.save(habilidade);
        });
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
