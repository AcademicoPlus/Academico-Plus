package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.habilidade.AdicionarHabilidadeUsuarioRequest;
import com.unifapi.academico.dto.habilidade.AtualizarNivelHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.UsuarioHabilidadeResponse;
import com.unifapi.academico.service.UsuarioHabilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios/me/habilidades")
@RequiredArgsConstructor
public class UsuarioHabilidadeController {

    private final UsuarioHabilidadeService usuarioHabilidadeService;

    @GetMapping
    @Operation(summary = "Listar minhas habilidades", description = "Tags de competência do usuário autenticado")
    public ResponseEntity<List<UsuarioHabilidadeResponse>> listar(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(usuarioHabilidadeService.listar(usuarioLogado.getId()));
    }

    @PostMapping
    @Operation(summary = "Adicionar habilidade ao perfil",
            description = "Vincula uma habilidade já existente no catálogo ao usuário autenticado")
    public ResponseEntity<UsuarioHabilidadeResponse> adicionar(
            @Valid @RequestBody AdicionarHabilidadeUsuarioRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        UsuarioHabilidadeResponse vinculo = usuarioHabilidadeService.adicionar(usuarioLogado.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vinculo);
    }

    @PatchMapping("/{habilidadeId}")
    @Operation(summary = "Atualizar nível de uma habilidade",
            description = "Ajusta o nível de proficiência (INICIANTE, INTERMEDIARIO, AVANCADO, EXPERT)")
    public ResponseEntity<UsuarioHabilidadeResponse> atualizarNivel(
            @PathVariable UUID habilidadeId,
            @Valid @RequestBody AtualizarNivelHabilidadeRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(usuarioHabilidadeService.atualizarNivel(
                usuarioLogado.getId(), habilidadeId, request.nivel()));
    }

    @DeleteMapping("/{habilidadeId}")
    @Operation(summary = "Remover habilidade do perfil")
    public ResponseEntity<MensagemResponse> remover(
            @PathVariable UUID habilidadeId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        usuarioHabilidadeService.remover(usuarioLogado.getId(), habilidadeId);
        return ResponseEntity.ok(new MensagemResponse(true, "Habilidade removida do seu perfil com sucesso."));
    }
}
