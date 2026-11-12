package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.projeto.AvisoProjetoResponse;
import com.unifapi.academico.service.AvisoProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Avisos de projeto (cancelado, concluído) para os membros da equipe. */
@RestController
@RequestMapping("/avisos")
@RequiredArgsConstructor
public class AvisoController {

    private final AvisoProjetoService avisoProjetoService;

    @GetMapping
    @Operation(summary = "Listar meus avisos pendentes",
            description = "Avisos de projetos de que sou membro (cancelado, concluído) ainda não fechados, do mais antigo ao mais recente")
    public ResponseEntity<List<AvisoProjetoResponse>> listarPendentes(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(avisoProjetoService.listarPendentes(usuarioLogado.getId()));
    }

    @PatchMapping("/{id}/visualizado")
    @Operation(summary = "Marcar aviso como visto", description = "Some da lista de pendentes. Só o destinatário do aviso pode marcar")
    public ResponseEntity<Void> marcarComoVisualizado(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        avisoProjetoService.marcarComoVisualizado(id, usuarioLogado.getId());
        return ResponseEntity.noContent().build();
    }
}
