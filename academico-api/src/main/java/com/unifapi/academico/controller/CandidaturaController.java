package com.unifapi.academico.controller;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.candidatura.AceitarCandidaturaRequest;
import com.unifapi.academico.dto.candidatura.CandidaturaResponse;
import com.unifapi.academico.dto.candidatura.CriarCandidaturaRequest;
import com.unifapi.academico.dto.candidatura.RejeitarCandidaturaRequest;
import com.unifapi.academico.service.CandidaturaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/candidaturas")
@RequiredArgsConstructor
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    // ============================================
    // VISÃO DO CANDIDATO
    // ============================================

    @PostMapping
    @Operation(summary = "Candidatar-se a um projeto",
            description = "Registra a candidatura do usuário logado. O projeto precisa estar ABERTO e com vaga disponível")
    public ResponseEntity<CandidaturaResponse> candidatar(
            @Valid @RequestBody CriarCandidaturaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        CandidaturaResponse criada = candidaturaService.candidatar(request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping("/minhas")
    @Operation(summary = "Listar minhas candidaturas",
            description = "Candidaturas enviadas pelo usuário logado, com filtro opcional por status")
    public ResponseEntity<Page<CandidaturaResponse>> listarMinhas(
            @RequestParam(required = false) StatusCandidatura status,
            @AuthenticationPrincipal Usuario usuarioLogado,
            @ParameterObject @PageableDefault(size = 10, sort = "dataCandidatura", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(candidaturaService.listarMinhas(usuarioLogado.getId(), status, pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar candidatura",
            description = "Desistência do próprio candidato. Permitido apenas enquanto a candidatura estiver PENDENTE")
    public ResponseEntity<MensagemResponse> cancelar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        candidaturaService.cancelar(id, usuarioLogado.getId());
        return ResponseEntity.ok(new MensagemResponse(true, "Candidatura cancelada com sucesso."));
    }

    // ============================================
    // VISÃO DO CRIADOR DO PROJETO
    // ============================================

    @GetMapping("/projeto/{projetoId}")
    @Operation(summary = "Listar candidaturas de um projeto",
            description = "Candidaturas recebidas por um projeto. Somente o criador do projeto tem acesso")
    public ResponseEntity<Page<CandidaturaResponse>> listarPorProjeto(
            @PathVariable UUID projetoId,
            @RequestParam(required = false) StatusCandidatura status,
            @AuthenticationPrincipal Usuario usuarioLogado,
            @ParameterObject @PageableDefault(size = 10, sort = "dataCandidatura", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                candidaturaService.listarPorProjeto(projetoId, status, pageable, usuarioLogado.getId()));
    }

    @PatchMapping("/{id}/aceitar")
    @Operation(summary = "Aceitar candidatura",
            description = "Aceita o candidato, cria o vínculo de membro e ocupa uma vaga do projeto. "
                    + "O corpo é opcional e permite definir a função do novo membro")
    public ResponseEntity<CandidaturaResponse> aceitar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AceitarCandidaturaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(candidaturaService.aceitar(id, request, usuarioLogado.getId()));
    }

    @PatchMapping("/{id}/rejeitar")
    @Operation(summary = "Rejeitar candidatura",
            description = "Rejeita o candidato registrando o motivo. Não consome vaga do projeto")
    public ResponseEntity<CandidaturaResponse> rejeitar(
            @PathVariable UUID id,
            @Valid @RequestBody RejeitarCandidaturaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(candidaturaService.rejeitar(id, request, usuarioLogado.getId()));
    }

    // ============================================
    // COMUM
    // ============================================

    @GetMapping("/{id}")
    @Operation(summary = "Buscar candidatura por ID",
            description = "Acessível ao próprio candidato e ao criador do projeto")
    public ResponseEntity<CandidaturaResponse> buscarPorId(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(candidaturaService.buscarPorId(id, usuarioLogado.getId()));
    }
}
