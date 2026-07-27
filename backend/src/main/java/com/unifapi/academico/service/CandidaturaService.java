package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.Candidatura;
import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.repository.CandidaturaRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidaturaService {

    private final CandidaturaRepository candidaturaRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
  

    /**
     * Criar candidatura (usuário se candidata a projeto)
     */
    public Candidatura criar(UUID projetoId, UUID usuarioId, String mensagem) {
        Projeto projeto = projetoRepository.findById(projetoId)
            .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Verificar se já se candidatou
        Optional<Candidatura> existente = candidaturaRepository
            .findByProjetoIdAndUsuarioId(projetoId, usuarioId);
        
        if (existente.isPresent()) {
            throw new RuntimeException("Usuário já se candidatou a este projeto");
        }

        // Verificar se projeto ainda tem vagas
        if (projeto.getVagasPreenchidas() >= projeto.getVagas()) {
            throw new RuntimeException("Projeto não possui vagas disponíveis");
        }

        Candidatura candidatura = Candidatura.builder()
            .projeto(projeto)
            .usuario(usuario)
            .status(StatusCandidatura.PENDENTE)
            .mensagem(mensagem)
            .dataCandidatura(LocalDateTime.now())
            .build();

        return candidaturaRepository.save(candidatura);
    }

    /**
     * Buscar candidatura por ID
     */
    public Optional<Candidatura> findById(UUID id) {
        return candidaturaRepository.findById(id);
    }

    /**
     * Listar candidaturas de um projeto
     */
    public List<Candidatura> findByProjetoId(UUID projetoId) {
        return candidaturaRepository.findByProjetoId(projetoId);
    }

    /**
     * Listar candidaturas pendentes de um projeto
     */
    public List<Candidatura> findPendentesByProjetoId(UUID projetoId) {
        List<Candidatura> todasCandidaturas = candidaturaRepository.findByProjetoId(projetoId);
        return todasCandidaturas.stream()
            .filter(c -> c.getStatus() == StatusCandidatura.PENDENTE)
            .toList();
    }

    /**
     * Aceitar candidatura
     */
    public Candidatura aceitar(UUID candidaturaId) {
        Candidatura candidatura = candidaturaRepository.findById(candidaturaId)
            .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        if (candidatura.getStatus() != StatusCandidatura.PENDENTE) {
            throw new RuntimeException("Candidatura já foi respondida");
        }

        Projeto projeto = candidatura.getProjeto();
        
        // Verificar vagas
        if (projeto.getVagasPreenchidas() >= projeto.getVagas()) {
            throw new RuntimeException("Projeto não possui vagas disponíveis");
        }

        // Atualizar candidatura
        candidatura.setStatus(StatusCandidatura.ACEITO);
        candidatura.setDataResposta(LocalDateTime.now());
        
        // Incrementar vagas preenchidas
        projeto.setVagasPreenchidas(projeto.getVagasPreenchidas() + 1);
        projetoRepository.save(projeto);

        return candidaturaRepository.save(candidatura);
    }

    /**
     * Rejeitar candidatura
     */
    public Candidatura rejeitar(UUID candidaturaId, String motivo) {
        Candidatura candidatura = candidaturaRepository.findById(candidaturaId)
            .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        if (candidatura.getStatus() != StatusCandidatura.PENDENTE) {
            throw new RuntimeException("Candidatura já foi respondida");
        }

        candidatura.setStatus(StatusCandidatura.REJEITADO);
        candidatura.setMotivoRejeicao(motivo);
        candidatura.setDataResposta(LocalDateTime.now());

        return candidaturaRepository.save(candidatura);
    }

    /**
     * Listar candidaturas por status
     */
    public List<Candidatura> findByStatus(StatusCandidatura status) {
        return candidaturaRepository.findByStatus(status);
    }

    /**
     * Contar candidaturas pendentes de um projeto
     */
    public long countPendentesByProjetoId(UUID projetoId) {
        return candidaturaRepository.findByProjetoId(projetoId).stream()
            .filter(c -> c.getStatus() == StatusCandidatura.PENDENTE)
            .count();
    }
}