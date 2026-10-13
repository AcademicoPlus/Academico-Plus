package com.unifapi.academico.controller;

import com.unifapi.academico.domain.enums.TipoAlvoDenuncia;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.denuncia.DenunciaResponse;
import com.unifapi.academico.dto.denuncia.DenunciarRequest;
import com.unifapi.academico.dto.denuncia.ResolverDenunciaRequest;
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

    // ============================================
    // DENUNCIAR (qualquer usuário autenticado)
    // ============================================

    @PostMapping("/avaliacoes/{avaliacaoId}/denuncias")
    @Operation(summary = "Denunciar uma avaliação recebida",
            description = "Só a pessoa avaliada pode denunciar a avaliação, uma vez por avaliação")
    public ResponseEntity<DenunciaResponse> denunciarAvaliacao(
            @PathVariable UUID avaliacaoId,
            @Valid @RequestBody DenunciarRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        DenunciaResponse denuncia = denunciaService.denunciarAvaliacao(avaliacaoId, request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(denuncia);
    }

    @PostMapping("/projetos/{projetoId}/denuncias")
    @Operation(summary = "Denunciar um projeto",
            description = "Qualquer usuário autenticado, menos o próprio criador, uma vez por projeto")
    public ResponseEntity<DenunciaResponse> denunciarProjeto(
            @PathVariable UUID projetoId,
            @Valid @RequestBody DenunciarRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        DenunciaResponse denuncia = denunciaService.denunciarProjeto(projetoId, request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(denuncia);
    }

    @PostMapping("/usuarios/{usuarioId}/denuncias")
    @Operation(summary = "Denunciar o perfil de outro usuário",
            description = "Qualquer usuário autenticado, menos o dono do perfil, uma vez por perfil")
    public ResponseEntity<DenunciaResponse> denunciarPerfil(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody DenunciarRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        DenunciaResponse denuncia = denunciaService.denunciarPerfil(usuarioId, request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(denuncia);
    }

    // ============================================
    // FILA DE MODERAÇÃO (ADMIN)
    // ============================================

    @GetMapping("/denuncias")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar denúncias pendentes (Admin)",
            description = "Opcionalmente filtra por tipo de alvo. Cada item traz o histórico de "
                    + "sanções do responsável e a medida sugerida pela escada de moderação")
    public ResponseEntity<Page<DenunciaResponse>> listarPendentes(
            @RequestParam(required = false) TipoAlvoDenuncia tipoAlvo,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(denunciaService.listarPendentes(tipoAlvo, pageable));
    }

    @PatchMapping("/denuncias/{id}/resolver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolver uma denúncia (Admin)",
            description = "O admin escolhe a medida: ARQUIVAR (improcedente), REMOVER_CONTEUDO, "
                    + "ADVERTENCIA ou SUSPENSAO. Advertir e suspender também removem o conteúdo "
                    + "denunciado e exigem justificativa — é o texto que o sancionado lê no login")
    public ResponseEntity<DenunciaResponse> resolver(
            @PathVariable UUID id,
            @Valid @RequestBody ResolverDenunciaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(denunciaService.resolver(id, request, usuarioLogado.getId()));
    }
}
