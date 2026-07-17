package com.unifapi.academico.api.controllers;

import com.unifapi.academico.service.RecomendacaoService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projetos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecomendacaoController {

    private final RecomendacaoService recomendacaoService;

    /**
     * GET /api/projetos/{id}/recomendacoes - Obter recomendações de colaboradores
     */
    @GetMapping("/{id}/recomendacoes")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Obter recomendações de colaboradores", description = "Retorna uma lista de recomendações de colaboradores para o projeto especificado.")
    public ResponseEntity<List<Map<String, Object>>> obterRecomendacoes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            List<Map<String, Object>> recomendacoes = recomendacaoService.obterRecomendacoes(id, limit);
            return ResponseEntity.ok(recomendacoes);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}