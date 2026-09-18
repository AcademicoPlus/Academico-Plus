package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.MensagemProjeto;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.mensagem.EnviarMensagemRequest;
import com.unifapi.academico.dto.mensagem.MensagemProjetoResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.MensagemProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Sala de chat de um projeto: acessível ao criador e a todo membro aceito
 * (existência de linha em ProjetoMembro), a mesma dupla de checagem já usada
 * em CandidaturaService/ProjetoMembroService para o restante da equipe.
 */
@Service
@RequiredArgsConstructor
public class ChatProjetoService {

    private final MensagemProjetoRepository mensagemProjetoRepository;
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
