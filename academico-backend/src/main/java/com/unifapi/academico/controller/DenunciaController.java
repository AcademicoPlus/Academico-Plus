package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.avaliacao.DenunciaResponse;
import com.unifapi.academico.dto.avaliacao.DenunciarAvaliacaoRequest;
import com.unifapi.academico.service.DenunciaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DenunciaController {

    private final DenunciaService denunciaService;

    @PostMapping("/avaliacoes/{avaliacaoId}/denuncias")
    @Operation(summary = "Denunciar uma avaliação recebida",
            description = "Só a pessoa avaliada pode denunciar a avaliação, uma vez por avaliação")
    public ResponseEntity<DenunciaResponse> denunciar(
            @PathVariable UUID avaliacaoId,
            @Valid @RequestBody DenunciarAvaliacaoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        DenunciaResponse denuncia = denunciaService.denunciar(avaliacaoId, request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(denuncia);
    }

    @GetMapping("/denuncias")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar denúncias pendentes (Admin)")
    public ResponseEntity<Page<DenunciaResponse>> listarPendentes(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(denunciaService.listarPendentes(pageable));
    }

    @PatchMapping("/denuncias/{id}/resolver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolver uma denúncia (Admin)",
            description = "Se procedente=true, a avaliação denunciada é removida e a nota do avaliado é recalculada")
    public ResponseEntity<DenunciaResponse> resolver(
            @PathVariable UUID id,
            @RequestParam boolean procedente,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(denunciaService.resolver(id, procedente, usuarioLogado.getId()));
    }
}
