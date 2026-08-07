package com.unifapi.academico.controller;

import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.habilidade.AtualizarHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.CriarHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.HabilidadeResponse;
import com.unifapi.academico.service.HabilidadeService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Catálogo de habilidades. Leitura liberada a qualquer usuário autenticado
 * (é preciso consultar o catálogo para montar um projeto ou o próprio perfil);
 * escrita restrita a ADMIN, já que o catálogo é compartilhado.
 */
@RestController
@RequestMapping("/habilidades")
@RequiredArgsConstructor
public class HabilidadeController {

    private final HabilidadeService habilidadeService;

    // ============================================
    // LEITURA
    // ============================================

    @GetMapping
    @Operation(summary = "Listar habilidades",
            description = "Lista paginada do catálogo, com busca por nome e filtro por categoria")
    public ResponseEntity<Page<HabilidadeResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String categoria,
            @ParameterObject @PageableDefault(size = 20, sort = "nome") Pageable pageable
    ) {
        return ResponseEntity.ok(habilidadeService.listar(busca, categoria, pageable));
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorias de habilidades",
            description = "Retorna as categorias distintas já cadastradas, úteis para montar filtros na interface")
    public ResponseEntity<List<String>> listarCategorias() {
        return ResponseEntity.ok(habilidadeService.listarCategorias());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar habilidade por ID")
    public ResponseEntity<HabilidadeResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(habilidadeService.buscarPorId(id));
    }

    // ============================================
    // ADMINISTRAÇÃO
    // ============================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar habilidade (Admin)",
            description = "Cadastra uma nova habilidade no catálogo. O nome é único")
    public ResponseEntity<HabilidadeResponse> criar(@Valid @RequestBody CriarHabilidadeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(habilidadeService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar habilidade (Admin)")
    public ResponseEntity<HabilidadeResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarHabilidadeRequest request
    ) {
        return ResponseEntity.ok(habilidadeService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover habilidade (Admin)",
            description = "Exclusão física, permitida apenas quando a habilidade não está vinculada a projetos nem a usuários")
    public ResponseEntity<MensagemResponse> remover(@PathVariable UUID id) {
        habilidadeService.remover(id);
        return ResponseEntity.ok(new MensagemResponse(true, "Habilidade removida com sucesso."));
    }
}
