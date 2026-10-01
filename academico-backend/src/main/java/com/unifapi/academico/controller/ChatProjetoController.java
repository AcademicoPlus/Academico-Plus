package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.mensagem.EnviarMensagemRequest;
import com.unifapi.academico.dto.mensagem.MensagemProjetoResponse;
import com.unifapi.academico.service.ChatProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatProjetoController {

    private final ChatProjetoService chatProjetoService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/projetos/{projetoId}/mensagens")
    @Operation(summary = "Histórico do chat do projeto",
            description = "Mensagens trocadas no projeto. Acessível ao criador e aos membros aceitos")
    public List<MensagemProjetoResponse> listarHistorico(
            @PathVariable UUID projetoId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return chatProjetoService.listarHistorico(projetoId, usuarioLogado.getId());
    }

    /**
     * Recebe a mensagem publicada em /app/projetos/{projetoId}/mensagens (ver
     * WebSocketConfig), persiste e retransmite para todos os inscritos em
     * /topic/projetos/{projetoId}. O Principal aqui vem do StompAuthChannelInterceptor,
     * que autentica o CONNECT — não do filtro de segurança HTTP normal.
     */
    @MessageMapping("/projetos/{projetoId}/mensagens")
    public void receberMensagem(
            @DestinationVariable UUID projetoId,
            @Payload EnviarMensagemRequest request,
            Principal principal
    ) {
        Usuario usuarioLogado = extrairUsuario(principal);
        MensagemProjetoResponse resposta = chatProjetoService.enviar(projetoId, usuarioLogado.getId(), request);
        messagingTemplate.convertAndSend("/topic/projetos/" + projetoId, resposta);
    }

    private Usuario extrairUsuario(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        throw new IllegalStateException("Usuário não autenticado na sessão WebSocket.");
    }
}
