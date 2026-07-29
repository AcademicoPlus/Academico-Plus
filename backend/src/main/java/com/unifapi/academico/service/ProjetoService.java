package com.unifapi.academico.service;

import com.unifapi.academico.api.dto.request.ProjetoRequestDTO;
import com.unifapi.academico.api.dto.response.ProjetoCardDTO;
import com.unifapi.academico.api.dto.response.ProjetoDetailResponseDTO;
import com.unifapi.academico.api.dto.response.ProjetoPageResponseDTO;
import com.unifapi.academico.api.dto.response.UserDTO;
import com.unifapi.academico.api.mapper.ProjetoMapper;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.model.Habilidade;
import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.model.ProjetoHabilidade;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.exception.AccessDeniedException;
import com.unifapi.academico.repository.HabilidadeRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
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
    private final ProjetoMapper mapper;

    /**
     * Criar novo projeto vinculado ao usuário e habilidades
     * ✅ FIX: Usa variáveis diferentes para evitar erro de escopo
     */
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

        // 2. Cria a entidade Projeto SEM habilidades
        Projeto projetoNovo = Projeto.builder()
                .titulo(dto.getTitulo())
                .descricao(dto.getDescricao())
                .vagas(dto.getVagas())
                .vagasPreenchidas(0)
                .dataFim(dto.getDataFim())
                .status(StatusProjeto.ABERTO)
                .ativo(true)
                .criador(criador)
                .habilidadesNecessarias(new HashSet<>())
                .build();

        // 3. SALVA PRIMEIRO (gera o ID do projeto)
        Projeto projetoSalvo = projetoRepository.saveAndFlush(projetoNovo);

        // 4. Associa as habilidades com o projeto salvo
        if (dto.getHabilidadeIds() != null && !dto.getHabilidadeIds().isEmpty()) {
            List<UUID> idsUnicos = dto.getHabilidadeIds().stream()
                    .distinct()
                    .toList();

            List<Habilidade> habilidades = habilidadeRepository.findAllById(idsUnicos);

            // Cria vinculos com Projeto já persistido
            Set<ProjetoHabilidade> vinculos = habilidades.stream()
                    .map(h -> ProjetoHabilidade.builder()
                            .projeto(projetoSalvo) // ✅ projetoSalvo é 'effectively final' agora
                            .habilidade(h)
                            .build())
                    .collect(Collectors.toSet());

            // Adiciona à coleção do objeto persistido
            projetoSalvo.getHabilidadesNecessarias().addAll(vinculos);

            // ✅ Salva novamente SEM reatribuir a variável projetoSalvo
            projetoRepository.saveAndFlush(projetoSalvo);
        }

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
     * Converter Projeto para ProjetoDetailResponseDTO
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
    @Transactional
    public ProjetoCardDTO atualizarProjeto(UUID id, ProjetoRequestDTO dto, String emailUsuarioLogado) {
        // 1. Busca o projeto existente
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com o ID: " + id));

        // 2. Valida se o usuário logado é o criador do projeto
        if (!projeto.getCriador().getEmail().equals(emailUsuarioLogado)) {
            throw new AccessDeniedException("Você não tem permissão para alterar este projeto.");
        }

        // 3. Atualiza os dados básicos
        projeto.setTitulo(dto.getTitulo());
        projeto.setDescricao(dto.getDescricao());
        projeto.setVagas(dto.getVagas());
        projeto.setDataFim(dto.getDataFim());

        // 4. Atualiza as habilidades (se informado no DTO)
        if (dto.getHabilidadeIds() != null) {
            // A) Limpa a coleção atual
            projeto.getHabilidadesNecessarias().clear();

            // B) FORÇA O HIBERNATE A EXECUTAR OS DELETES
            projetoRepository.saveAndFlush(projeto);

            // C) Se não vazio, associa novas
            if (!dto.getHabilidadeIds().isEmpty()) {
                // Remove IDs duplicados
                List<UUID> idsUnicos = dto.getHabilidadeIds().stream()
                        .distinct()
                        .toList();

                List<Habilidade> habilidades = habilidadeRepository.findAllById(idsUnicos);

                Set<ProjetoHabilidade> novosVinculos = habilidades.stream()
                        .map(h -> ProjetoHabilidade.builder()
                                .projeto(projeto)
                                .habilidade(h)
                                .obrigatoria(true)
                                .build())
                        .collect(Collectors.toSet());

                projeto.getHabilidadesNecessarias().addAll(novosVinculos);
            }
        }

        // D) Salva as novas alterações
        Projeto salvo = projetoRepository.saveAndFlush(projeto);
        return mapper.toCardDTO(salvo);
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