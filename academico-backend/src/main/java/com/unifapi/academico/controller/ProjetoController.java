package com.unifapi.academico.controller;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.projeto.AlterarStatusProjetoRequest;
import com.unifapi.academico.dto.projeto.AtualizarProjetoRequest;
import com.unifapi.academico.dto.projeto.CriarProjetoRequest;
import com.unifapi.academico.dto.projeto.ProjetoResponse;
import com.unifapi.academico.dto.projeto.ProjetoResumoResponse;
import com.unifapi.academico.service.ProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/projetos")
@RequiredArgsConstructor
public class ProjetoController {

    private final ProjetoService projetoService;

    // ============================================
    // ESCRITA
    // ============================================

    @PostMapping
    @Operation(summary = "Criar novo projeto",
            description = "Cadastra um novo projeto vinculado ao usuário logado. Aceita opcionalmente a lista de habilidades necessárias")
    public ResponseEntity<ProjetoResponse> criar(
            @Valid @RequestBody CriarProjetoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        ProjetoResponse criado = projetoService.criar(request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar projeto",
            description = "Atualiza os dados de um projeto existente. Somente o criador pode alterar")
    public ResponseEntity<ProjetoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarProjetoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(projetoService.atualizar(id, request, usuarioLogado.getId()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status do projeto",
            description = "Altera o status do projeto (ABERTO, EM_ANDAMENTO, CONCLUIDO, CANCELADO). "
                    + "Ao cancelar, os membros recebem um aviso. Projetos concluídos ou cancelados são finais")
    public ResponseEntity<ProjetoResponse> alterarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarStatusProjetoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(projetoService.alterarStatus(id, request, usuarioLogado.getId()));
    }

    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload da capa do projeto",
            description = "Recebe um arquivo de imagem e atualiza a capa/banner do projeto. Somente o criador pode alterar")
    public ResponseEntity<ProjetoResponse> uploadBanner(
            @PathVariable UUID id,
            @RequestParam("banner") MultipartFile arquivo,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(projetoService.atualizarBanner(id, usuarioLogado.getId(), arquivo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar projeto",
            description = "Exclusão lógica do projeto, preservando o histórico de candidaturas e membros")
    public ResponseEntity<MensagemResponse> inativar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        projetoService.inativar(id, usuarioLogado.getId());
        return ResponseEntity.ok(new MensagemResponse(true, "Projeto inativado com sucesso."));
    }

    // ============================================
    // LEITURA
    // ============================================

    @GetMapping
    @Operation(summary = "Listar projetos",
            description = "Lista paginada de projetos ativos, com filtro opcional por status e busca por título/descrição")
    public ResponseEntity<Page<ProjetoResumoResponse>> listar(
            @RequestParam(required = false) StatusProjeto status,
            @RequestParam(required = false) String busca,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(projetoService.listar(status, busca, pageable));
    }

    @GetMapping("/meus-projetos")
    @Operation(summary = "Listar projetos que eu criei",
            description = "Retorna os projetos ativos criados pelo usuário autenticado")
    public ResponseEntity<Page<ProjetoResumoResponse>> listarMeusProjetos(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(projetoService.listarPorCriador(usuarioLogado.getId(), pageable));
    }

    @GetMapping("/participando")
    @Operation(summary = "Listar projetos em que eu participo",
            description = "Retorna os projetos ativos em que o usuário autenticado é membro da equipe")
    public ResponseEntity<Page<ProjetoResumoResponse>> listarParticipando(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(projetoService.listarParticipando(usuarioLogado.getId(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar projeto por ID",
            description = "Detalhes do projeto, incluindo habilidades necessárias e totais de membros e candidaturas pendentes")
    public ResponseEntity<ProjetoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(projetoService.buscarPorId(id));
    }
}
