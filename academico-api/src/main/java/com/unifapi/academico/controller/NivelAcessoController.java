package com.unifapi.academico.controller;

import com.unifapi.academico.dto.usuario.NivelAcessoResponse;
import com.unifapi.academico.service.NivelAcessoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/niveis-acesso")
@RequiredArgsConstructor
public class NivelAcessoController {

    private final NivelAcessoService nivelAcessoService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar níveis de acesso (Admin)", description = "Usado para preencher o seletor de promoção de usuário")
    public ResponseEntity<List<NivelAcessoResponse>> listar() {
        return ResponseEntity.ok(nivelAcessoService.listarTodos());
    }
}
