package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.LeituraChatProjeto;
import com.unifapi.academico.domain.models.MensagemProjeto;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.mensagem.ConversaResponse;
import com.unifapi.academico.dto.mensagem.EnviarMensagemRequest;
import com.unifapi.academico.dto.mensagem.MensagemProjetoResponse;
import com.unifapi.academico.dto.mensagem.NaoLidasPorProjeto;
import com.unifapi.academico.dto.projeto.ProjetoRefResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.LeituraChatProjetoRepository;
import com.unifapi.academico.repository.MensagemProjetoRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sala de chat de um projeto: acessível ao criador e a todo membro aceito
 * (existência de linha em ProjetoMembro), a mesma dupla de checagem já usada
 * em CandidaturaService/ProjetoMembroService para o restante da equipe.
 */
@Service
@RequiredArgsConstructor
public class ChatProjetoService {

    private final MensagemProjetoRepository mensagemProjetoRepository;
    private final LeituraChatProjetoRepository leituraChatProjetoRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProjetoService projetoService;
    private final ProjetoMembroService projetoMembroService;

    @Transactional(readOnly = true)
    public List<MensagemProjetoResponse> listarHistorico(UUID projetoId, UUID usuarioLogadoId) {
        Projeto projeto = obterProjetoComAcesso(projetoId, usuarioLogadoId);

        return mensagemProjetoRepository.findByProjeto_IdOrderByCriadoEmAsc(projeto.getId()).stream()
                .map(MensagemProjetoResponse::fromEntity)
                .toList();
    }

    /**
     * Recebida tanto pelo controller REST (com @Valid já aplicado no DTO)
     * quanto pelo @MessageMapping do STOMP, que não passa pelo pipeline de
     * bean validation do MVC — por isso a checagem de conteúdo é repetida aqui.
     */
    @Transactional
    public MensagemProjetoResponse enviar(UUID projetoId, UUID usuarioLogadoId, EnviarMensagemRequest request) {
        Projeto projeto = obterProjetoComAcesso(projetoId, usuarioLogadoId);

        String conteudo = request.conteudo() != null ? request.conteudo().trim() : "";
        if (conteudo.isEmpty()) {
            throw new RegraDeNegocioException("A mensagem não pode ser vazia.");
        }
        if (conteudo.length() > 4000) {
            throw new RegraDeNegocioException("A mensagem deve ter no máximo 4000 caracteres.");
        }

        Usuario autor = usuarioRepository.findById(usuarioLogadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        MensagemProjeto mensagem = mensagemProjetoRepository.save(MensagemProjeto.builder()
                .projeto(projeto)
                .autor(autor)
                .conteudo(conteudo)
                .build());

        return MensagemProjetoResponse.fromEntity(mensagem);
    }

    // ============================================
    // CAIXA "MENSAGENS"
    // ============================================

    /**
     * Uma conversa por projeto em que o usuário é criador ou membro, com a
     * última mensagem e o total de não lidas. Ordena pela atividade mais
     * recente; projetos sem mensagens vão para o fim, do mais novo ao mais antigo.
     */
    @Transactional(readOnly = true)
    public List<ConversaResponse> listarConversas(UUID usuarioLogadoId) {
        List<Projeto> projetos = projetoRepository.listarVinculados(usuarioLogadoId);
        if (projetos.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = projetos.stream().map(Projeto::getId).toList();

        Map<UUID, MensagemProjeto> ultimas = mensagemProjetoRepository.buscarUltimasMensagens(ids).stream()
                .collect(Collectors.toMap(m -> m.getProjeto().getId(), Function.identity(), (a, b) -> a));
        Map<UUID, Long> naoLidas = naoLidasPorProjeto(usuarioLogadoId, ids);

        Comparator<Projeto> porAtividade = Comparator.comparing(
                (Projeto p) -> ultimas.containsKey(p.getId()) ? ultimas.get(p.getId()).getCriadoEm() : p.getCriadoEm(),
                Comparator.nullsLast(Comparator.reverseOrder()));

        return projetos.stream()
                .sorted(Comparator.comparing((Projeto p) -> !ultimas.containsKey(p.getId())).thenComparing(porAtividade))
                .map(p -> new ConversaResponse(
                        ProjetoRefResponse.fromEntity(p),
                        p.getBannerUrl(),
                        ultimas.containsKey(p.getId()) ? MensagemProjetoResponse.fromEntity(ultimas.get(p.getId())) : null,
                        naoLidas.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(UUID usuarioLogadoId) {
        List<UUID> ids = projetoRepository.listarVinculados(usuarioLogadoId).stream().map(Projeto::getId).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        return naoLidasPorProjeto(usuarioLogadoId, ids).values().stream().mapToLong(Long::longValue).sum();
    }

    /** Chamado ao abrir a conversa e ao receber mensagem com ela aberta. */
    @Transactional
    public void marcarComoLida(UUID projetoId, UUID usuarioLogadoId) {
        Projeto projeto = obterProjetoComAcesso(projetoId, usuarioLogadoId);

        LeituraChatProjeto leitura = leituraChatProjetoRepository
                .findByProjeto_IdAndUsuario_Id(projetoId, usuarioLogadoId)
                .orElseGet(() -> LeituraChatProjeto.builder()
                        .projeto(projeto)
                        .usuario(usuarioRepository.getReferenceById(usuarioLogadoId))
                        .build());

        leitura.setLidoAte(LocalDateTime.now());
        leituraChatProjetoRepository.save(leitura);
    }

    private Map<UUID, Long> naoLidasPorProjeto(UUID usuarioId, List<UUID> projetoIds) {
        return mensagemProjetoRepository.contarNaoLidas(usuarioId, projetoIds).stream()
                .collect(Collectors.toMap(NaoLidasPorProjeto::projetoId, NaoLidasPorProjeto::total));
    }

    // ============================================
    // PRIVADOS
    // ============================================

    private Projeto obterProjetoComAcesso(UUID projetoId, UUID usuarioLogadoId) {
        Projeto projeto = projetoService.obterEntidadeAtiva(projetoId);

        boolean temAcesso = projetoService.ehCriador(projeto, usuarioLogadoId)
                || projetoMembroService.ehMembro(projetoId, usuarioLogadoId);

        if (!temAcesso) {
            throw new RegraDeNegocioException("Você não tem acesso ao chat deste projeto.");
        }

        return projeto;
    }
}
