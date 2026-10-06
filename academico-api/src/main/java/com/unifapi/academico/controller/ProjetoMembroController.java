package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.membro.AdicionarMembroRequest;
import com.unifapi.academico.dto.membro.AtualizarFuncaoMembroRequest;
import com.unifapi.academico.dto.membro.ProjetoMembroResponse;
import com.unifapi.academico.service.ProjetoMembroService;
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
@RequestMapping("/projetos/{projetoId}/membros")
@RequiredArgsConstructor
public class ProjetoMembroController {

    private final ProjetoMembroService projetoMembroService;

    @GetMapping
    @Operation(summary = "Listar membros do projeto",
            description = "Retorna a equipe do projeto, ordenada pela data de adesão")
    public ResponseEntity<List<ProjetoMembroResponse>> listar(@PathVariable UUID projetoId) {
        return ResponseEntity.ok(projetoMembroService.listarPorProjeto(projetoId));
    }

    @GetMapping("/{membroId}")
    @Operation(summary = "Buscar membro do projeto por ID")
    public ResponseEntity<ProjetoMembroResponse> buscarPorId(
            @PathVariable UUID projetoId,
            @PathVariable UUID membroId
    ) {
        return ResponseEntity.ok(projetoMembroService.buscarPorId(projetoId, membroId));
    }

    @PostMapping
    @Operation(summary = "Adicionar membro ao projeto",
            description = "Inclusão direta pelo criador do projeto, sem passar por candidatura. Ocupa uma vaga")
    public ResponseEntity<ProjetoMembroResponse> adicionar(
            @PathVariable UUID projetoId,
            @Valid @RequestBody AdicionarMembroRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        ProjetoMembroResponse membro = projetoMembroService.adicionar(projetoId, request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(membro);
    }

    @PatchMapping("/{membroId}/funcao")
    @Operation(summary = "Alterar função do membro",
            description = "Define o papel do membro na equipe. Somente o criador do projeto pode alterar")
    public ResponseEntity<ProjetoMembroResponse> atualizarFuncao(
            @PathVariable UUID projetoId,
            @PathVariable UUID membroId,
            @Valid @RequestBody AtualizarFuncaoMembroRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(
                projetoMembroService.atualizarFuncao(projetoId, membroId, request, usuarioLogado.getId()));
    }

    @DeleteMapping("/{membroId}")
    @Operation(summary = "Remover membro do projeto",
            description = "Libera a vaga ocupada. Permitido ao criador do projeto ou ao próprio membro (sair do projeto)")
    public ResponseEntity<MensagemResponse> remover(
            @PathVariable UUID projetoId,
            @PathVariable UUID membroId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        projetoMembroService.remover(projetoId, membroId, usuarioLogado.getId());
        return ResponseEntity.ok(new MensagemResponse(true, "Membro removido do projeto com sucesso."));
    }
}
