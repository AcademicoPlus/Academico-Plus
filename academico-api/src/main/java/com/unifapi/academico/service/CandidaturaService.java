package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Candidatura;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.candidatura.AceitarCandidaturaRequest;
import com.unifapi.academico.dto.candidatura.CandidaturaResponse;
import com.unifapi.academico.dto.candidatura.CriarCandidaturaRequest;
import com.unifapi.academico.dto.candidatura.RejeitarCandidaturaRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CandidaturaRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fluxo de candidatura a projeto.
 *
 * PENDENTE é o único estado editável: aceitar/rejeitar são terminais, e o
 * candidato só pode cancelar enquanto estiver pendente. O aceite delega a
 * criação do vínculo a ProjetoMembroService, que é quem controla as vagas.
 */
@Service
@RequiredArgsConstructor
public class CandidaturaService {

    private final CandidaturaRepository candidaturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProjetoService projetoService;
    private final ProjetoMembroService projetoMembroService;

    // ============================================
    // CANDIDATO
    // ============================================

    @Transactional
    public CandidaturaResponse candidatar(CriarCandidaturaRequest request, UUID usuarioLogadoId) {
        Projeto projeto = projetoService.obterEntidadeAtiva(request.projetoId());

        Usuario usuario = usuarioRepository.findById(usuarioLogadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (projetoService.ehCriador(projeto, usuarioLogadoId)) {
            throw new RegraDeNegocioException("Você não pode se candidatar a um projeto criado por você.");
        }

        if (projeto.getStatus() != StatusProjeto.ABERTO) {
            throw new RegraDeNegocioException("Este projeto não está aberto para candidaturas (status atual: "
                    + projeto.getStatus().getLabel() + ").");
        }

        if (!projeto.podeAceitarCandidato()) {
            throw new RegraDeNegocioException("Este projeto já preencheu todas as vagas disponíveis.");
        }

        if (projetoMembroService.ehMembro(projeto.getId(), usuarioLogadoId)) {
            throw new RegraDeNegocioException("Você já é membro deste projeto.");
        }

        if (candidaturaRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), usuarioLogadoId)) {
            throw new RegraDeNegocioException("Você já possui uma candidatura registrada para este projeto.");
        }

        Candidatura candidatura = candidaturaRepository.save(Candidatura.builder()
                .projeto(projeto)
                .usuario(usuario)
                .status(StatusCandidatura.PENDENTE)
                .mensagem(normalizar(request.mensagem()))
                .dataCandidatura(LocalDateTime.now())
                .build());

        return CandidaturaResponse.fromEntity(candidatura);
    }

    @Transactional(readOnly = true)
    public Page<CandidaturaResponse> listarMinhas(UUID usuarioLogadoId, StatusCandidatura status, Pageable pageable) {
        Page<Candidatura> candidaturas = (status != null)
                ? candidaturaRepository.findByUsuario_IdAndStatus(usuarioLogadoId, status, pageable)
                : candidaturaRepository.findByUsuario_Id(usuarioLogadoId, pageable);

        return candidaturas.map(CandidaturaResponse::fromEntity);
    }

    /** Desistência: só é permitida enquanto a candidatura estiver pendente. */
    @Transactional
    public void cancelar(UUID candidaturaId, UUID usuarioLogadoId) {
        Candidatura candidatura = obterEntidade(candidaturaId);

        if (candidatura.getUsuario() == null || !candidatura.getUsuario().getId().equals(usuarioLogadoId)) {
            throw new RegraDeNegocioException("Você só pode cancelar as suas próprias candidaturas.");
        }

        if (candidatura.getStatus() != StatusCandidatura.PENDENTE) {
            throw new RegraDeNegocioException("Esta candidatura já foi "
                    + candidatura.getStatus().getLabel().toLowerCase() + " e não pode ser cancelada.");
        }

        candidaturaRepository.delete(candidatura);
    }

    // ============================================
    // CRIADOR DO PROJETO
    // ============================================

    @Transactional(readOnly = true)
    public Page<CandidaturaResponse> listarPorProjeto(UUID projetoId,
                                                      StatusCandidatura status,
                                                      Pageable pageable,
                                                      UUID usuarioLogadoId) {
        // Só o criador vê a lista de candidatos do seu projeto.
        projetoService.obterEntidadeComoDono(projetoId, usuarioLogadoId);

        Page<Candidatura> candidaturas = (status != null)
                ? candidaturaRepository.findByProjeto_IdAndStatus(projetoId, status, pageable)
                : candidaturaRepository.findByProjeto_Id(projetoId, pageable);

        return candidaturas.map(CandidaturaResponse::fromEntity);
    }

    @Transactional
    public CandidaturaResponse aceitar(UUID candidaturaId,
                                       AceitarCandidaturaRequest request,
                                       UUID usuarioLogadoId) {
        Candidatura candidatura = obterEntidade(candidaturaId);
        Projeto projeto = obterProjetoDaCandidaturaComoDono(candidatura, usuarioLogadoId);

        validarPendente(candidatura);

        // Valida vagas/duplicidade e sincroniza vagasPreenchidas.
        projetoMembroService.adicionarMembro(projeto, candidatura.getUsuario(),
                request != null ? request.funcao() : null);

        candidatura.setStatus(StatusCandidatura.ACEITO);
        candidatura.setMotivoRejeicao(null);
        candidatura.setDataResposta(LocalDateTime.now());

        return CandidaturaResponse.fromEntity(candidaturaRepository.save(candidatura));
    }

    @Transactional
    public CandidaturaResponse rejeitar(UUID candidaturaId,
                                        RejeitarCandidaturaRequest request,
                                        UUID usuarioLogadoId) {
        Candidatura candidatura = obterEntidade(candidaturaId);
        obterProjetoDaCandidaturaComoDono(candidatura, usuarioLogadoId);

        validarPendente(candidatura);

        candidatura.setStatus(StatusCandidatura.REJEITADO);
        candidatura.setMotivoRejeicao(request.motivoRejeicao().trim());
        candidatura.setDataResposta(LocalDateTime.now());

        return CandidaturaResponse.fromEntity(candidaturaRepository.save(candidatura));
    }

    // ============================================
    // COMUM
    // ============================================

    /** Visível ao criador do projeto e ao próprio candidato. */
    @Transactional(readOnly = true)
    public CandidaturaResponse buscarPorId(UUID candidaturaId, UUID usuarioLogadoId) {
        Candidatura candidatura = obterEntidade(candidaturaId);

        boolean ehCandidato = candidatura.getUsuario() != null
                && candidatura.getUsuario().getId().equals(usuarioLogadoId);
        boolean ehDonoDoProjeto = candidatura.getProjeto() != null
                && projetoService.ehCriador(candidatura.getProjeto(), usuarioLogadoId);

        if (!ehCandidato && !ehDonoDoProjeto) {
            throw new RegraDeNegocioException("Você não tem permissão para visualizar esta candidatura.");
        }

        return CandidaturaResponse.fromEntity(candidatura);
    }

    // ============================================
    // PRIVADOS
    // ============================================

    private Candidatura obterEntidade(UUID candidaturaId) {
        return candidaturaRepository.findById(candidaturaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Candidatura não encontrada com o ID informado."));
    }

    private Projeto obterProjetoDaCandidaturaComoDono(Candidatura candidatura, UUID usuarioLogadoId) {
        if (candidatura.getProjeto() == null) {
            throw new RecursoNaoEncontradoException("Projeto da candidatura não encontrado.");
        }
        return projetoService.obterEntidadeComoDono(candidatura.getProjeto().getId(), usuarioLogadoId);
    }

    private void validarPendente(Candidatura candidatura) {
        if (candidatura.getStatus() != StatusCandidatura.PENDENTE) {
            throw new RegraDeNegocioException("Esta candidatura já foi respondida (status atual: "
                    + candidatura.getStatus().getLabel() + ").");
        }
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
