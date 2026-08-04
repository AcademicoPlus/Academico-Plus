package com.unifapi.academico.controller;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.projeto.AtualizarProjetoRequest;
import com.unifapi.academico.dto.projeto.CriarProjetoRequest;
import com.unifapi.academico.dto.projeto.ProjetoResponse;
import com.unifapi.academico.service.ProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/projetos")
@RequiredArgsConstructor
public class ProjetoController {

    private final ProjetoService projetoService;

    @PostMapping
    @Operation(summary = "Criar novo projeto", description = "Cadastra um novo projeto vinculado ao usuário logado")
    public ResponseEntity<ProjetoResponse> criar(
            @Valid @RequestBody CriarProjetoRequest request,
            @AuthenticationPrincipal UUID usuarioLogadoId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.criar(request, usuarioLogadoId));
    }

    @GetMapping
    @Operation(summary = "Listar projetos", description = "Retorna uma lista paginada de projetos ativos, com opção de filtro por status")
    public ResponseEntity<Page<ProjetoResponse>> listar(
            @RequestParam(required = false) StatusProjeto status,
            @PageableDefault(size = 10, sort = "criadoEm") Pageable pageable
    ) {
        return ResponseEntity.ok(projetoService.listar(status, pageable));
    }

    @GetMapping("/meus-projetos")
    @Operation(summary = "Listar projetos do criador", description = "Retorna os projetos criados pelo usuário autenticado")
    public ResponseEntity<Page<ProjetoResponse>> listarMeusProjetos(
            @AuthenticationPrincipal UUID usuarioLogadoId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(projetoService.listarPorCriador(usuarioLogadoId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar projeto por ID", description = "Exibe os detalhes de um projeto ativo específico")
    public ResponseEntity<ProjetoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(projetoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar projeto", description = "Atualiza os dados de um projeto existente (somente o criador pode alterar)")
    public ResponseEntity<ProjetoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarProjetoRequest request,
            @AuthenticationPrincipal UUID usuarioLogadoId
    ) {
        return ResponseEntity.ok(projetoService.atualizar(id, request, usuarioLogadoId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status do projeto", description = "Altera o status (ex: ABERTO, FECHADO, CONCLUIDO) do projeto")
    public ResponseEntity<ProjetoResponse> alterarStatus(
            @PathVariable UUID id,
            @RequestParam StatusProjeto status,
            @AuthenticationPrincipal UUID usuarioLogadoId
    ) {
        return ResponseEntity.ok(projetoService.alterarStatus(id, status, usuarioLogadoId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar projeto", description = "Realiza a exclusão lógica (inativação) do projeto")
    public ResponseEntity<MensagemResponse> inativar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID usuarioLogadoId
    ) {
        projetoService.inativar(id, usuarioLogadoId);
        return ResponseEntity.ok(new MensagemResponse(true, "Projeto inativado com sucesso."));
    }
}