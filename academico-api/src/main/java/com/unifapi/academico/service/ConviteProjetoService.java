package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.enums.StatusConvite;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.ConviteProjeto;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.convite.ConviteResponse;
import com.unifapi.academico.dto.convite.CriarConviteRequest;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CandidaturaRepository;
import com.unifapi.academico.repository.ConviteProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Convite de projeto: o criador convida, o convidado aceita ou recusa.
 *
 * PENDENTE é o único estado editável. O convite não reserva vaga — as vagas
 * são validadas de novo no aceite, que delega a criação do vínculo a
 * ProjetoMembroService.adicionarMembro(). Se o convidado tiver uma
 * candidatura pendente ao mesmo projeto, ela é aceita junto.
 */
@Service
@RequiredArgsConstructor
public class ConviteProjetoService {

    private final ConviteProjetoRepository conviteRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProjetoService projetoService;
    private final ProjetoMembroService projetoMembroService;

    // ============================================
    // CRIADOR DO PROJETO
    // ============================================

    @Transactional
    public ConviteResponse convidar(CriarConviteRequest request, UUID usuarioLogadoId) {
        Projeto projeto = projetoService.obterEntidadeComoDono(request.projetoId(), usuarioLogadoId);

        Usuario convidado = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID informado."));

        if (projetoService.ehCriador(projeto, convidado.getId())) {
            throw new RegraDeNegocioException("Você não pode convidar a si mesmo para o seu projeto.");
        }

        if (projeto.getStatus() != StatusProjeto.ABERTO && projeto.getStatus() != StatusProjeto.EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Não é possível enviar convites para um projeto "
                    + projeto.getStatus().getLabel().toLowerCase() + ".");
        }

        if (projeto.getVagas() != null && projeto.getVagasPreenchidas() >= projeto.getVagas()) {
            throw new RegraDeNegocioException("O projeto já preencheu todas as vagas disponíveis.");
        }

        if (projetoMembroService.ehMembro(projeto.getId(), convidado.getId())) {
            throw new RegraDeNegocioException("Este usuário já é membro do projeto.");
        }

        if (conviteRepository.existsByProjeto_IdAndConvidado_IdAndStatus(
                projeto.getId(), convidado.getId(), StatusConvite.PENDENTE)) {
            throw new RegraDeNegocioException("Já existe um convite pendente para este usuário.");
        }

        if (candidaturaRepository.existsByProjeto_IdAndUsuario_IdAndStatus(
                projeto.getId(), convidado.getId(), StatusCandidatura.PENDENTE)) {
            throw new RegraDeNegocioException(
                    "Este usuário já se candidatou ao projeto. Aceite a candidatura em vez de enviar um convite.");
        }

        Usuario convidante = projeto.getCriador();

        ConviteProjeto convite = conviteRepository.save(ConviteProjeto.builder()
                .projeto(projeto)
                .convidado(convidado)
                .convidante(convidante)
                .funcao(normalizar(request.funcao()))
                .mensagem(normalizar(request.mensagem()))
                .status(StatusConvite.PENDENTE)
                .criadoEm(LocalDateTime.now())
                .build());

        return ConviteResponse.fromEntity(convite);
    }

    @Transactional(readOnly = true)
    public Page<ConviteResponse> listarPorProjeto(UUID projetoId,
                                                  StatusConvite status,
                                                  Pageable pageable,
                                                  UUID usuarioLogadoId) {
        projetoService.obterEntidadeComoDono(projetoId, usuarioLogadoId);

        Page<ConviteProjeto> convites = (status != null)
                ? conviteRepository.findByProjeto_IdAndStatus(projetoId, status, pageable)
                : conviteRepository.findByProjeto_Id(projetoId, pageable);

        return convites.map(ConviteResponse::fromEntity);
    }

    /** O criador retira um convite ainda não respondido. Fica como histórico (CANCELADO). */
    @Transactional
    public ConviteResponse cancelar(UUID conviteId, UUID usuarioLogadoId) {
        ConviteProjeto convite = obterEntidade(conviteId);
        projetoService.obterEntidadeComoDono(convite.getProjeto().getId(), usuarioLogadoId);

        validarPendente(convite);

        convite.setStatus(StatusConvite.CANCELADO);
        convite.setRespondidoEm(LocalDateTime.now());

        return ConviteResponse.fromEntity(conviteRepository.save(convite));
    }

    // ============================================
    // CONVIDADO
    // ============================================

    @Transactional(readOnly = true)
    public Page<ConviteResponse> listarRecebidos(UUID usuarioLogadoId, StatusConvite status, Pageable pageable) {
        Page<ConviteProjeto> convites = (status != null)
                ? conviteRepository.findByConvidado_IdAndStatusAndProjeto_AtivoTrue(usuarioLogadoId, status, pageable)
                : conviteRepository.findByConvidado_IdAndProjeto_AtivoTrue(usuarioLogadoId, pageable);

        return convites.map(ConviteResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long contarPendentes(UUID usuarioLogadoId) {
        return conviteRepository.countByConvidado_IdAndStatusAndProjeto_AtivoTrue(
                usuarioLogadoId, StatusConvite.PENDENTE);
    }

    @Transactional
    public ConviteResponse aceitar(UUID conviteId, UUID usuarioLogadoId) {
        ConviteProjeto convite = obterEntidadeComoConvidado(conviteId, usuarioLogadoId);
        validarPendente(convite);

        Projeto projeto = projetoService.obterEntidadeAtiva(convite.getProjeto().getId());

        // Valida status do projeto, vagas e duplicidade, e sincroniza vagasPreenchidas.
        projetoMembroService.adicionarMembro(projeto, convite.getConvidado(), convite.getFuncao());

        candidaturaRepository.findByProjeto_IdAndUsuario_IdAndStatus(
                        projeto.getId(), usuarioLogadoId, StatusCandidatura.PENDENTE)
                .ifPresent(candidatura -> {
                    candidatura.setStatus(StatusCandidatura.ACEITO);
                    candidatura.setDataResposta(LocalDateTime.now());
                    candidaturaRepository.save(candidatura);
                });

        convite.setStatus(StatusConvite.ACEITO);
        convite.setRespondidoEm(LocalDateTime.now());

        return ConviteResponse.fromEntity(conviteRepository.save(convite));
    }

    @Transactional
    public ConviteResponse recusar(UUID conviteId, UUID usuarioLogadoId) {
        ConviteProjeto convite = obterEntidadeComoConvidado(conviteId, usuarioLogadoId);
        validarPendente(convite);

        convite.setStatus(StatusConvite.RECUSADO);
        convite.setRespondidoEm(LocalDateTime.now());

        return ConviteResponse.fromEntity(conviteRepository.save(convite));
    }

    // ============================================
    // COMUM
    // ============================================

    /** Visível ao convidado e ao criador do projeto. */
    @Transactional(readOnly = true)
    public ConviteResponse buscarPorId(UUID conviteId, UUID usuarioLogadoId) {
        ConviteProjeto convite = obterEntidade(conviteId);

        if (!ehConvidado(convite, usuarioLogadoId) && !projetoService.ehCriador(convite.getProjeto(), usuarioLogadoId)) {
            throw new RegraDeNegocioException("Você não tem permissão para visualizar este convite.");
        }

        return ConviteResponse.fromEntity(convite);
    }

    // ============================================
    // PRIVADOS
    // ============================================

    private ConviteProjeto obterEntidade(UUID conviteId) {
        ConviteProjeto convite = conviteRepository.findById(conviteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado com o ID informado."));

        if (convite.getProjeto() == null) {
            throw new RecursoNaoEncontradoException("Projeto do convite não encontrado.");
        }

        return convite;
    }

    private ConviteProjeto obterEntidadeComoConvidado(UUID conviteId, UUID usuarioLogadoId) {
        ConviteProjeto convite = obterEntidade(conviteId);

        if (!ehConvidado(convite, usuarioLogadoId)) {
            throw new RegraDeNegocioException("Você só pode responder aos convites enviados para você.");
        }

        return convite;
    }

    private static boolean ehConvidado(ConviteProjeto convite, UUID usuarioId) {
        return convite.getConvidado() != null && convite.getConvidado().getId().equals(usuarioId);
    }

    private void validarPendente(ConviteProjeto convite) {
        if (convite.getStatus() != StatusConvite.PENDENTE) {
            throw new RegraDeNegocioException("Este convite já foi "
                    + convite.getStatus().getLabel().toLowerCase() + ".");
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
