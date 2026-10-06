package com.unifapi.academico.controller;

import com.unifapi.academico.dto.usuario.CursoResponse;
import com.unifapi.academico.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lista pública: precisa estar acessível antes do login para alimentar o formulário de cadastro. */
@RestController
@RequestMapping("/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursoService;

    @GetMapping
    @Operation(summary = "Listar cursos ativos", description = "Retorna os cursos disponíveis para seleção no cadastro")
    public ResponseEntity<List<CursoResponse>> listar() {
        return ResponseEntity.ok(cursoService.listarAtivos());
    }
}
