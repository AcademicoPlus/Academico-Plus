package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.habilidade.ProjetoHabilidadeResponse;
import com.unifapi.academico.dto.habilidade.VincularHabilidadeRequest;
import com.unifapi.academico.service.ProjetoHabilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projetos/{projetoId}/habilidades")
@RequiredArgsConstructor
public class ProjetoHabilidadeController {

    private final ProjetoHabilidadeService projetoHabilidadeService;

    @GetMapping
    @Operation(summary = "Listar habilidades necessárias do projeto",
            description = "Habilidades exigidas ou desejáveis para participar do projeto")
    public ResponseEntity<List<ProjetoHabilidadeResponse>> listar(@PathVariable UUID projetoId) {
        return ResponseEntity.ok(projetoHabilidadeService.listarPorProjeto(projetoId));
    }

    @PostMapping
    @Operation(summary = "Vincular habilidade ao projeto",
            description = "Vincula uma habilidade já existente no catálogo. Somente o criador do projeto pode alterar")
    public ResponseEntity<ProjetoHabilidadeResponse> vincular(
            @PathVariable UUID projetoId,
            @Valid @RequestBody VincularHabilidadeRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        ProjetoHabilidadeResponse vinculo =
                projetoHabilidadeService.vincular(projetoId, request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(vinculo);
    }

    @PatchMapping("/{habilidadeId}")
    @Operation(summary = "Alterar obrigatoriedade da habilidade",
            description = "Define se a habilidade é obrigatória (true) ou apenas desejável (false) no projeto")
    public ResponseEntity<ProjetoHabilidadeResponse> alterarObrigatoriedade(
            @PathVariable UUID projetoId,
            @PathVariable UUID habilidadeId,
            @RequestParam boolean obrigatoria,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(projetoHabilidadeService.alterarObrigatoriedade(
                projetoId, habilidadeId, obrigatoria, usuarioLogado.getId()));
    }

    @DeleteMapping("/{habilidadeId}")
    @Operation(summary = "Desvincular habilidade do projeto",
            description = "Remove a habilidade da lista de requisitos do projeto. A habilidade permanece no catálogo")
    public ResponseEntity<MensagemResponse> desvincular(
            @PathVariable UUID projetoId,
            @PathVariable UUID habilidadeId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        projetoHabilidadeService.desvincular(projetoId, habilidadeId, usuarioLogado.getId());
        return ResponseEntity.ok(new MensagemResponse(true, "Habilidade desvinculada do projeto com sucesso."));
    }
}
