package com.unifapi.academico.service;

import com.unifapi.academico.api.dto.request.ProjetoRequestDTO;
import com.unifapi.academico.api.dto.response.ProjetoCardDTO;
import com.unifapi.academico.api.dto.response.ProjetoDetailResponseDTO;
import com.unifapi.academico.api.dto.response.ProjetoPageResponseDTO;
import com.unifapi.academico.api.dto.response.UserDTO;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.model.Habilidade;
import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.model.ProjetoHabilidade;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.repository.HabilidadeRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HabilidadeRepository habilidadeRepository;

    /**
     * Criar novo projeto vinculado ao usuário e habilidades
     */
    public ProjetoCardDTO criarProjeto(ProjetoRequestDTO dto, String emailCriador) {
        if (dto.getVagas() != null && dto.getVagas() < 1) {
            throw new IllegalArgumentException("O projeto deve ter no mínimo 1 vaga.");
        }

        // 1. Busca o criador pelo e-mail do token
        Usuario criador = usuarioRepository.findByEmail(emailCriador)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + emailCriador));

        // 2. Monta a entidade Projeto
        Projeto projeto = Projeto.builder()
            .titulo(dto.getTitulo())
            .descricao(dto.getDescricao())
            .vagas(dto.getVagas())
            .vagasPreenchidas(0)
            .dataFim(dto.getDataFim())
            .status(StatusProjeto.ABERTO)
            .ativo(true)
            .criador(criador)
            .build();

        // 3. Associa as habilidades recebidas
        if (dto.getHabilidadeIds() != null && !dto.getHabilidadeIds().isEmpty()) {
            List<Habilidade> habilidades = habilidadeRepository.findAllById(dto.getHabilidadeIds());

            Set<ProjetoHabilidade> vinculos = habilidades.stream()
                .map(h -> ProjetoHabilidade.builder()
                    .projeto(projeto)
                    .habilidade(h)
                    .build())
                .collect(Collectors.toSet());

            projeto.setHabilidadesNecessarias(vinculos);
        }

        // 4. Salva no banco e retorna o CardDTO montado
        Projeto projetoSalvo = projetoRepository.save(projeto);
        return ProjetoCardDTO.from(projetoSalvo);
    }

    /**
     * Buscar projeto por ID com detalhes
     */
    @Transactional(readOnly = true)
    public Optional<ProjetoDetailResponseDTO> findByIdDetail(UUID id) {
        return projetoRepository.findById(id)
            .map(this::converterParaDetalhe);
    }

    /**
     * Converter Projeto para ProjetoDetailResponseDTO (Com verificações de segurança para nulos)
     */
    private ProjetoDetailResponseDTO converterParaDetalhe(Projeto projeto) {
        UserDTO criadorDTO = (projeto.getCriador() != null) ? UserDTO.from(projeto.getCriador()) : null;

        List<String> habilidades = (projeto.getHabilidadesNecessarias() == null) 
            ? List.of() 
            : projeto.getHabilidadesNecessarias().stream()
                .filter(ph -> ph != null && ph.getHabilidade() != null)
                .map(ph -> ph.getHabilidade().getNome())
                .toList();

        List<UserDTO> membrosList = (projeto.getMembros() == null) 
            ? List.of() 
            : projeto.getMembros().stream()
                .filter(pm -> pm != null && pm.getUsuario() != null)
                .map(pm -> UserDTO.from(pm.getUsuario()))
                .toList();

        int totalCandidatos = (projeto.getCandidaturas() == null) ? 0 : projeto.getCandidaturas().size();

        return ProjetoDetailResponseDTO.builder()
            .id(projeto.getId())
            .titulo(projeto.getTitulo())
            .descricao(projeto.getDescricao())
            .status(projeto.getStatus())
            .vagas(projeto.getVagas())
            .vagasPreenchidas(projeto.getVagasPreenchidas())
            .dataFim(projeto.getDataFim())
            .criadoEm(projeto.getCriadoEm())
            .criador(criadorDTO)
            .habilidadesNecessarias(habilidades)
            .membros(membrosList)
            .totalCandidatos(totalCandidatos)
            .build();
    }

    /**
     * Listar projetos por status com paginação
     */
    @Transactional(readOnly = true)
    public ProjetoPageResponseDTO findByStatusWithDetails(StatusProjeto status, Pageable pageable) {
        Page<Projeto> page = projetoRepository.findByStatus(status, pageable);
        List<ProjetoCardDTO> dados = page.getContent().stream()
            .map(ProjetoCardDTO::from)
            .toList();
        return ProjetoPageResponseDTO.from(page, dados);
    }

    /**
     * Listar todos os projetos ativos com paginação
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<Projeto> findByCriador(UUID criadorId) {
        return projetoRepository.findByCriadorId(criadorId);
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
     * Decrementar vagas ao aceitar um candidato
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