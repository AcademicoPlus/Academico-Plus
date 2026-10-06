package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.mensagem.ConversaResponse;
import com.unifapi.academico.dto.mensagem.TotalNaoLidasResponse;
import com.unifapi.academico.service.ChatProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Caixa "Mensagens": visão das conversas de todos os projetos do usuário. */
@RestController
@RequestMapping("/conversas")
@RequiredArgsConstructor
public class ConversaController {

    private final ChatProjetoService chatProjetoService;

    @GetMapping
    @Operation(summary = "Listar minhas conversas",
            description = "Uma conversa por projeto em que sou criador ou membro, com a última mensagem e o total de não lidas")
    public ResponseEntity<List<ConversaResponse>> listar(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(chatProjetoService.listarConversas(usuarioLogado.getId()));
    }

    @GetMapping("/nao-lidas")
    @Operation(summary = "Total de mensagens não lidas", description = "Usado pelo badge do menu lateral")
    public ResponseEntity<TotalNaoLidasResponse> contarNaoLidas(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(new TotalNaoLidasResponse(chatProjetoService.contarNaoLidas(usuarioLogado.getId())));
    }

    @PutMapping("/{projetoId}/lida")
    @Operation(summary = "Marcar conversa como lida", description = "Registra a leitura até agora. Exige ser criador ou membro")
    public ResponseEntity<Void> marcarComoLida(
            @PathVariable UUID projetoId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        chatProjetoService.marcarComoLida(projetoId, usuarioLogado.getId());
        return ResponseEntity.noContent().build();
    }
}
