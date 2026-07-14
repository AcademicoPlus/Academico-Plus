package com.unifapi.academico.api.controllers;

import com.unifapi.academico.domain.model.Candidatura;
import com.unifapi.academico.service.CandidaturaService;

import io.swagger.v3.oas.annotations.Operation;

import com.unifapi.academico.api.dto.request.CreateCandidaturaRequestDTO;
import com.unifapi.academico.api.dto.request.RespondeCandidaturaRequestDTO;
import com.unifapi.academico.api.dto.response.CandidaturaDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projetos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    /**
     * POST /api/projetos/{projetoId}/candidaturas - Criar candidatura
     */
    @PostMapping("/{projetoId}/candidaturas")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar candidatura", description = "Cria uma nova candidatura para um projeto específico")
    public ResponseEntity<CandidaturaDTO> criar(
            @PathVariable Long projetoId,
            @Valid @RequestBody CreateCandidaturaRequestDTO request) {
        try {
            // TODO: Pegar usuarioId do token JWT
            Long usuarioId = 1L; // HARDCODED POR ENQUANTO
            
            Candidatura candidatura = candidaturaService.criar(projetoId, usuarioId, request.getMensagem());
            return ResponseEntity.status(HttpStatus.CREATED).body(CandidaturaDTO.from(candidatura));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/projetos/{projetoId}/candidaturas - Listar candidaturas de um projeto
     */
    @GetMapping("/{projetoId}/candidaturas")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar candidaturas", description = "Retorna uma lista de candidaturas para um projeto específico")
    public ResponseEntity<List<CandidaturaDTO>> listar(@PathVariable Long projetoId) {
        List<CandidaturaDTO> dtos = candidaturaService.findByProjetoId(projetoId).stream()
            .map(CandidaturaDTO::from)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/projetos/{projetoId}/candidaturas/pendentes - Listar candidaturas PENDENTES
     */
    @GetMapping("/{projetoId}/candidaturas/pendentes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar candidaturas pendentes", description = "Retorna uma lista de candidaturas pendentes para um projeto específico")
    public ResponseEntity<List<CandidaturaDTO>> listarPendentes(@PathVariable Long projetoId) {
        List<CandidaturaDTO> dtos = candidaturaService.findPendentesByProjetoId(projetoId).stream()
            .map(CandidaturaDTO::from)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * PUT /api/projetos/{projetoId}/candidaturas/{candidaturaId}/aceitar - Aceitar candidatura
     */
    @PutMapping("/{projetoId}/candidaturas/{candidaturaId}/aceitar")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Aceitar candidatura", description = "Aceita uma candidatura específica para um projeto")
    public ResponseEntity<CandidaturaDTO> aceitar(
            @PathVariable Long projetoId,
            @PathVariable Long candidaturaId) {
        try {
            Candidatura candidatura = candidaturaService.aceitar(candidaturaId);
            return ResponseEntity.ok(CandidaturaDTO.from(candidatura));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/projetos/{projetoId}/candidaturas/{candidaturaId}/rejeitar - Rejeitar candidatura
     */
    @PutMapping("/{projetoId}/candidaturas/{candidaturaId}/rejeitar")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Rejeitar candidatura", description = "Rejeita uma candidatura específica para um projeto, com motivo")
    public ResponseEntity<CandidaturaDTO> rejeitar(
            @PathVariable Long projetoId,
            @PathVariable Long candidaturaId,
            @Valid @RequestBody RespondeCandidaturaRequestDTO request) {
        try {
            Candidatura candidatura = candidaturaService.rejeitar(candidaturaId, request.getMotivo());
            return ResponseEntity.ok(CandidaturaDTO.from(candidatura));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}