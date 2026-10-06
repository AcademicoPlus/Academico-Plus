package com.unifapi.academico.controller;

import com.unifapi.academico.domain.enums.StatusConvite;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.convite.ContagemConvitesResponse;
import com.unifapi.academico.dto.convite.ConviteResponse;
import com.unifapi.academico.dto.convite.CriarConviteRequest;
import com.unifapi.academico.service.ConviteProjetoService;
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
@RequestMapping("/convites")
@RequiredArgsConstructor
public class ConviteProjetoController {

    private final ConviteProjetoService conviteService;

    // ============================================
    // VISÃO DO CRIADOR DO PROJETO
    // ============================================

    @PostMapping
    @Operation(summary = "Convidar usuário para um projeto",
            description = "Somente o criador pode convidar. O convite não reserva vaga; as vagas são validadas no aceite")
    public ResponseEntity<ConviteResponse> convidar(
            @Valid @RequestBody CriarConviteRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        ConviteResponse criado = conviteService.convidar(request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping("/projeto/{projetoId}")
    @Operation(summary = "Listar convites de um projeto",
            description = "Convites enviados pelo criador, com filtro opcional por status")
    public ResponseEntity<Page<ConviteResponse>> listarPorProjeto(
            @PathVariable UUID projetoId,
            @RequestParam(required = false) StatusConvite status,
            @AuthenticationPrincipal Usuario usuarioLogado,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(conviteService.listarPorProjeto(projetoId, status, pageable, usuarioLogado.getId()));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar convite",
            description = "O criador retira um convite ainda PENDENTE")
    public ResponseEntity<ConviteResponse> cancelar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(conviteService.cancelar(id, usuarioLogado.getId()));
    }

    // ============================================
    // VISÃO DO CONVIDADO
    // ============================================

    @GetMapping("/recebidos")
    @Operation(summary = "Listar convites recebidos",
            description = "Convites recebidos pelo usuário logado, com filtro opcional por status")
    public ResponseEntity<Page<ConviteResponse>> listarRecebidos(
            @RequestParam(required = false) StatusConvite status,
            @AuthenticationPrincipal Usuario usuarioLogado,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(conviteService.listarRecebidos(usuarioLogado.getId(), status, pageable));
    }

    @GetMapping("/recebidos/contagem")
    @Operation(summary = "Contar convites pendentes", description = "Usado pelo badge de notificações")
    public ResponseEntity<ContagemConvitesResponse> contarPendentes(
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(new ContagemConvitesResponse(conviteService.contarPendentes(usuarioLogado.getId())));
    }

    @PatchMapping("/{id}/aceitar")
    @Operation(summary = "Aceitar convite",
            description = "Cria o vínculo de membro e ocupa uma vaga do projeto")
    public ResponseEntity<ConviteResponse> aceitar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(conviteService.aceitar(id, usuarioLogado.getId()));
    }

    @PatchMapping("/{id}/recusar")
    @Operation(summary = "Recusar convite")
    public ResponseEntity<ConviteResponse> recusar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(conviteService.recusar(id, usuarioLogado.getId()));
    }

    // ============================================
    // COMUM
    // ============================================

    @GetMapping("/{id}")
    @Operation(summary = "Buscar convite por ID",
            description = "Acessível ao convidado e ao criador do projeto")
    public ResponseEntity<ConviteResponse> buscarPorId(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(conviteService.buscarPorId(id, usuarioLogado.getId()));
    }
}
