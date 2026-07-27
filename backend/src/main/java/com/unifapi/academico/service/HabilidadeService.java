package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.Habilidade;
import com.unifapi.academico.repository.HabilidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HabilidadeService {

    private final HabilidadeRepository habilidadeRepository;

    /**
     * Buscar habilidade por ID
     */
    public Optional<Habilidade> findById(UUID id) {
        return habilidadeRepository.findById(id);
    }

    /**
     * Buscar habilidade por nome (único)
     */
    public Optional<Habilidade> findByNome(String nome) {
        return habilidadeRepository.findByNome(nome);
    }

    /**
     * Listar todas as habilidades
     */
    public Page<Habilidade> findAll(Pageable pageable) {
        return habilidadeRepository.findAll(pageable);
    }

    /**
     * Listar todas as habilidades SEM paginação
     */
    public List<Habilidade> findAll() {
        return habilidadeRepository.findAll();
    }

    /**
     * Criar nova habilidade
     */
    public Habilidade create(Habilidade habilidade) {
        if (habilidade.getNome() == null || habilidade.getNome().isBlank()) {
            throw new RuntimeException("Nome da habilidade é obrigatório");
        }
        
        if (habilidadeRepository.findByNome(habilidade.getNome()).isPresent()) {
            throw new RuntimeException("Habilidade com este nome já existe");
        }
        
        return habilidadeRepository.save(habilidade);
    }

    /**
     * Atualizar habilidade
     */
    public Habilidade update(UUID id, Habilidade habilidadeAtualizada) {
        return habilidadeRepository.findById(id)
            .map(habilidade -> {
                habilidade.setNome(habilidadeAtualizada.getNome());
                habilidade.setCategoria(habilidadeAtualizada.getCategoria());
                habilidade.setDescricao(habilidadeAtualizada.getDescricao());
                return habilidadeRepository.save(habilidade);
            })
            .orElseThrow(() -> new RuntimeException("Habilidade não encontrada"));
    }

    /**
     * Deletar habilidade
     */
    public void delete(UUID id) {
        habilidadeRepository.deleteById(id);
    }

    /**
     * Incrementar contador de usuários
     */
    public void incrementarUsuariosCount(UUID id) {
        habilidadeRepository.findById(id)
            .ifPresent(h -> {
                h.setUsuariosCount(h.getUsuariosCount() + 1);
                habilidadeRepository.save(h);
            });
    }

    /**
     * Incrementar contador de projetos
     */
    public void incrementarProjetosCount(UUID id) {
        habilidadeRepository.findById(id)
            .ifPresent(h -> {
                h.setProjetosCount(h.getProjetosCount() + 1);
                habilidadeRepository.save(h);
            });
    }
}