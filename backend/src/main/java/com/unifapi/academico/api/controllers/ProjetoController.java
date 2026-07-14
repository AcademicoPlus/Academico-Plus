package com.unifapi.academico.api.controllers;

import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.service.ProjetoService;

import io.swagger.v3.oas.annotations.Operation;

import com.unifapi.academico.api.dto.response.ProjetoDetalheDTO;
import com.unifapi.academico.api.dto.response.ProjetoListResponseDTO;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projetos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjetoController {

    private final ProjetoService projetoService;

    /**
     * GET /api/projetos - Listar com filtros
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar projetos", description = "Retorna uma lista paginada de projetos, podendo filtrar por status")
    public ResponseEntity<ProjetoListResponseDTO> listar(
            @RequestParam(required = false) StatusProjeto status,
            @ParameterObject Pageable pageable) {
        
        ProjetoListResponseDTO response;
        
        if (status != null) {
            response = projetoService.findByStatusWithDetails(status, pageable);
        } else {
            response = projetoService.findAllWithDetails(pageable);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/projetos/{id} - Detalhes
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Obter detalhes do projeto", description = "Retorna os detalhes de um projeto específico pelo seu ID")
    public ResponseEntity<ProjetoDetalheDTO> obter(@PathVariable Long id) {
        return projetoService.findByIdDetail(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/projetos - Criar
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Criar novo projeto", description = "Cria um novo projeto no sistema")
    public ResponseEntity<Projeto> criar(@RequestBody Projeto projeto) {
        try {
            Projeto criado = projetoService.create(projeto);
            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/projetos/{id} - Atualizar
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Atualizar projeto", description = "Atualiza os dados de um projeto existente")
    public ResponseEntity<Projeto> atualizar(
            @PathVariable Long id,
            @RequestBody Projeto projeto) {
        try {
            Projeto atualizado = projetoService.update(id, projeto);
            return ResponseEntity.ok(atualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/projetos/{id} - Deletar
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Deletar projeto", description = "Remove um projeto do sistema")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        projetoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}