package com.unifapi.academico.api.controllers;

import com.unifapi.academico.domain.model.Habilidade;
import com.unifapi.academico.service.HabilidadeService;

import io.swagger.v3.oas.annotations.Operation;

import com.unifapi.academico.api.dto.request.CreateHabilidadeRequestDTO;
import com.unifapi.academico.api.dto.response.HabilidadeDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/habilidades")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HabilidadeController {

    private final HabilidadeService habilidadeService;

    /**
     * GET /api/habilidades - Listar com paginação
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar habilidades", description = "Retorna uma lista paginada de habilidades")
    public ResponseEntity<Page<HabilidadeDTO>> listar(
            @org.springframework.data.web.PageableDefault(sort = "nome", direction = org.springframework.data.domain.Sort.Direction.ASC) Pageable pageable) {
        
        Page<Habilidade> page = habilidadeService.findAll(pageable);
        List<HabilidadeDTO> dtos = page.getContent().stream()
            .map(HabilidadeDTO::from)
            .toList();
        Page<HabilidadeDTO> responsePage = new PageImpl<>(dtos, pageable, page.getTotalElements());
        return ResponseEntity.ok(responsePage);
    }

    /**
     * GET /api/habilidades/todas - Listar TODAS sem paginação
     */
    @GetMapping("/todas")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar todas as habilidades", description = "Retorna uma lista de todas as habilidades sem paginação")
    public ResponseEntity<List<HabilidadeDTO>> listarTodas() {
        List<HabilidadeDTO> dtos = habilidadeService.findAll().stream()
            .map(HabilidadeDTO::from)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/habilidades/{id} - Detalhes
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obter detalhes da habilidade", description = "Retorna os detalhes de uma habilidade específica pelo seu ID")
    public ResponseEntity<HabilidadeDTO> obter(@PathVariable Long id) {
        return habilidadeService.findById(id)
            .map(h -> ResponseEntity.ok(HabilidadeDTO.from(h)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/habilidades - Criar (ADMIN apenas)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar habilidade", description = "Cria uma nova habilidade (somente para administradores)")
    public ResponseEntity<HabilidadeDTO> criar(@Valid @RequestBody CreateHabilidadeRequestDTO request) {
        try {
            Habilidade habilidade = Habilidade.builder()
                .nome(request.getNome())
                .categoria(request.getCategoria())
                .descricao(request.getDescricao())
                .usuariosCount(0)
                .projetosCount(0)
                .build();
            
            Habilidade criada = habilidadeService.create(habilidade);
            return ResponseEntity.status(HttpStatus.CREATED).body(HabilidadeDTO.from(criada));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/habilidades/{id} - Atualizar (ADMIN apenas)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar habilidade", description = "Atualiza os dados de uma habilidade específica pelo seu ID (somente para administradores)")
    public ResponseEntity<HabilidadeDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CreateHabilidadeRequestDTO request) {
        try {
            Habilidade habilidade = Habilidade.builder()
                .nome(request.getNome())
                .categoria(request.getCategoria())
                .descricao(request.getDescricao())
                .build();
            
            Habilidade atualizada = habilidadeService.update(id, habilidade);
            return ResponseEntity.ok(HabilidadeDTO.from(atualizada));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/habilidades/{id} - Deletar (ADMIN apenas)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deletar habilidade", description = "Deleta uma habilidade específica pelo seu ID (somente para administradores)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        habilidadeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}