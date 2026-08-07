package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.recomendacao.ProjetoRecomendadoResponse;
import com.unifapi.academico.dto.recomendacao.UsuarioRecomendadoResponse;
import com.unifapi.academico.service.RecomendacaoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recomendacoes")
@RequiredArgsConstructor
public class RecomendacaoController {

    private final RecomendacaoService recomendacaoService;

    @GetMapping("/projetos")
    @Operation(summary = "Recomendar projetos para o usuário logado",
            description = "Ranqueia projetos ABERTOS pela cobertura dos requisitos de habilidade a partir das tags do usuário")
    public ResponseEntity<List<ProjetoRecomendadoResponse>> recomendarProjetos(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestParam(required = false) Integer limite
    ) {
        return ResponseEntity.ok(recomendacaoService.recomendarProjetos(usuarioLogado.getId(), limite));
    }

    @GetMapping("/projetos/{projetoId}/candidatos")
    @Operation(summary = "Recomendar candidatos para um projeto",
            description = "Somente o criador do projeto. Ranqueia usuários pela cobertura dos requisitos de habilidade do projeto")
    public ResponseEntity<List<UsuarioRecomendadoResponse>> recomendarCandidatos(
            @PathVariable UUID projetoId,
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestParam(required = false) Integer limite
    ) {
        return ResponseEntity.ok(recomendacaoService.recomendarCandidatos(projetoId, usuarioLogado.getId(), limite));
    }
}
