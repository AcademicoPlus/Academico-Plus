package com.unifapi.academico.api.controllers;

import com.unifapi.academico.domain.model.UsuarioHabilidade;
import com.unifapi.academico.service.UsuarioHabilidadeService;

import io.swagger.v3.oas.annotations.Operation;

import com.unifapi.academico.api.dto.request.AdicionarHabilidadeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios/{usuarioId}/habilidades")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UsuarioHabilidadeController {

    private final UsuarioHabilidadeService usuarioHabilidadeService;

    /**
     * GET /api/usuarios/{usuarioId}/habilidades - Listar todas as habilidades do usuário
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar habilidades de um usuário", description = "Lista todas as habilidades de um usuário específico.")
    public ResponseEntity<List<Map<String, Object>>> listar(@PathVariable UUID usuarioId) {
        try {
            List<UsuarioHabilidade> habilidades = usuarioHabilidadeService.listarHabilidades(usuarioId);
            
            List<Map<String, Object>> dtos = habilidades.stream()
                .map(uh -> Map.of(
                    "id", uh.getId(),
                    "habilidadeId", (Object) uh.getHabilidade().getId(),
                    "habilidadeNome", uh.getHabilidade().getNome(),
                    "nivel", uh.getNivel().toString(),
                    "endossado", uh.getEndossado(),
                    "endossosCount", uh.getEndossosCount()
                ))
                .toList();
            
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/usuarios/{usuarioId}/habilidades/{habilidadeId} - Obter uma habilidade específica
     */
    @GetMapping("/{habilidadeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obter habilidade de um usuário", description = "Obtém uma habilidade específica de um usuário.")
    public ResponseEntity<?> obter(
            @PathVariable UUID usuarioId,
            @PathVariable UUID habilidadeId) {
        try {
            var usuarioHabilidade = usuarioHabilidadeService.obterHabilidade(usuarioId, habilidadeId)
                .orElseThrow(() -> new RuntimeException("Habilidade não encontrada"));

            return ResponseEntity.ok(Map.of(
                "id", usuarioHabilidade.getId(),
                "habilidadeId", usuarioHabilidade.getHabilidade().getId(),
                "habilidadeNome", usuarioHabilidade.getHabilidade().getNome(),
                "nivel", usuarioHabilidade.getNivel().toString(),
                "endossado", usuarioHabilidade.getEndossado(),
                "endossosCount", usuarioHabilidade.getEndossosCount()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/usuarios/{usuarioId}/habilidades - Adicionar habilidade ao usuário
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adicionar habilidade a um usuário", description = "Adiciona uma habilidade a um usuário específico, ou atualiza o nível se já existir.")
    public ResponseEntity<?> adicionar(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody AdicionarHabilidadeDTO request) {
        try {
            UsuarioHabilidade usuarioHabilidade = usuarioHabilidadeService.adicionarHabilidade(
                usuarioId,
                request.getHabilidadeId(),
                request.getNivel()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", usuarioHabilidade.getId(),
                "usuarioId", usuarioHabilidade.getUsuario().getId(),
                "habilidadeId", usuarioHabilidade.getHabilidade().getId(),
                "habilidadeNome", usuarioHabilidade.getHabilidade().getNome(),
                "nivel", usuarioHabilidade.getNivel().toString(),
                "mensagem", "Habilidade adicionada com sucesso"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * PUT /api/usuarios/{usuarioId}/habilidades/{habilidadeId} - Atualizar nível da habilidade
     */
    @PutMapping("/{habilidadeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar nível de uma habilidade do usuário", description = "Atualiza o nível de uma habilidade específica de um usuário.")
    public ResponseEntity<?> atualizar(
            @PathVariable UUID usuarioId,
            @PathVariable UUID habilidadeId,
            @Valid @RequestBody AdicionarHabilidadeDTO request) {
        try {
            UsuarioHabilidade usuarioHabilidade = usuarioHabilidadeService.atualizarNivel(
                usuarioId,
                habilidadeId,
                request.getNivel()
            );

            return ResponseEntity.ok(Map.of(
                "id", usuarioHabilidade.getId(),
                "habilidadeId", usuarioHabilidade.getHabilidade().getId(),
                "habilidadeNome", usuarioHabilidade.getHabilidade().getNome(),
                "nivelAntigo", request.getNivel().toString(),
                "nivelNovo", usuarioHabilidade.getNivel().toString(),
                "mensagem", "Nível atualizado com sucesso"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * DELETE /api/usuarios/{usuarioId}/habilidades/{habilidadeId} - Remover habilidade
     */
    @DeleteMapping("/{habilidadeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Remover habilidade de um usuário", description = "Remove uma habilidade específica de um usuário.")
    public ResponseEntity<?> remover(
            @PathVariable UUID usuarioId,
            @PathVariable UUID habilidadeId) {
        try {
            usuarioHabilidadeService.removerHabilidade(usuarioId, habilidadeId);
            return ResponseEntity.ok(Map.of("mensagem", "Habilidade removida com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}