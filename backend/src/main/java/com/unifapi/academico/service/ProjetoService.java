package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.api.dto.response.ProjetoDetailResponseDTO;
import com.unifapi.academico.api.dto.response.ProjetoCardDTO;
import com.unifapi.academico.api.dto.response.ProjetoPageResponseDTO;
import com.unifapi.academico.api.dto.response.UserDTO;
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
public class ProjetoService {

    private final ProjetoRepository projetoRepository;

    /**
     * Buscar projeto por ID com detalhes
     */
    public Optional<ProjetoDetailResponseDTO> findByIdDetail(UUID id) {
        return projetoRepository.findById(id)
            .map(this::converterParaDetalhe);  // ← USE UM MÉTODO AUXILIAR
    }

    /**
     * Converter Projeto para ProjetoDetalheDTO
     */
    private ProjetoDetailResponseDTO converterParaDetalhe(Projeto projeto) {
        UserDTO criadorDTO = null;
        if (projeto.getCriador() != null) {
            criadorDTO = UserDTO.from(projeto.getCriador());
        }

        List<String> habilidades = projeto.getHabilidadesNecessarias().stream()
            .map(ph -> ph.getHabilidade().getNome())
            .toList();

        List<UserDTO> membrosList = projeto.getMembros().stream()
            .map(pm -> UserDTO.from(pm.getUsuario()))
            .toList();

        return ProjetoDetailResponseDTO.builder()
            .id(projeto.getId())
            .titulo(projeto.getTitulo())
            .descricao(projeto.getDescricao())
            .status(projeto.getStatus())
            .vagas(projeto.getVagas())
            .vagasPreenchidas(projeto.getVagasPreenchidas())
            .dataFim(projeto.getDataFim())
            .criadoEm(projeto.getCriadoEm())
            .criador(criadorDTO)  // ← SEM PROBLEMAS DE CAST
            .habilidadesNecessarias(habilidades)
            .membros(membrosList)  // ← SEM PROBLEMAS DE CAST
            .totalCandidatos(projeto.getCandidaturas().size())
            .build();
    }

    /**
     * Listar todos os projetos
     */
    public ProjetoPageResponseDTO findByStatusWithDetails(StatusProjeto status, Pageable pageable) {
    Page<Projeto> page = projetoRepository.findByStatus(status, pageable);
    List<ProjetoCardDTO> dados = page.getContent().stream()
        .map(ProjetoCardDTO::from)
        .toList();
    return ProjetoPageResponseDTO.from(page, dados);
    }

    public ProjetoPageResponseDTO findAllWithDetails(Pageable pageable) {
    Page<Projeto> page = projetoRepository.findByAtivoTrue(pageable);
    List<ProjetoCardDTO> dados = page.getContent().stream()
        .map(ProjetoCardDTO::from)
        .toList();
    return ProjetoPageResponseDTO.from(page, dados);
    }

    /**
     * Listar projetos por criador
     */
    public List<Projeto> findByCriador(UUID criadorId) {
        return projetoRepository.findByCriadorId(criadorId);
    }

    /**
     * Listar projetos por status
     */
    public Page<Projeto> findByStatus(StatusProjeto status, Pageable pageable) {
        return projetoRepository.findByStatus(status, pageable);
    }

    /**
     * Criar novo projeto
     */
    public Projeto create(Projeto projeto) {
        if (projeto.getVagas() < 1) {
            throw new RuntimeException("Projeto deve ter no mínimo 1 vaga");
        }
        return projetoRepository.save(projeto);
    }

    /**
     * Atualizar projeto
     */
    public Projeto update(UUID id, Projeto projetoAtualizado) {
        return projetoRepository.findById(id)
            .map(projeto -> {
                projeto.setTitulo(projetoAtualizado.getTitulo());
                projeto.setDescricao(projetoAtualizado.getDescricao());
                projeto.setStatus(projetoAtualizado.getStatus());
                projeto.setVagas(projetoAtualizado.getVagas());
                return projetoRepository.save(projeto);
            })
            .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
    }

    /**
     * Deletar projeto
     */
    public void delete(UUID id) {
        projetoRepository.deleteById(id);
    }

    /**
     * Decrementar vagas
     */
    public void decrementarVagas(UUID projetoId) {
        projetoRepository.findById(projetoId)
            .ifPresent(projeto -> {
                if (projeto.getVagasPreenchidas() < projeto.getVagas()) {
                    projeto.setVagasPreenchidas(projeto.getVagasPreenchidas() + 1);
                    projetoRepository.save(projeto);
                }
            });
    }
}