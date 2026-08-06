package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.usuario.UsuarioPerfilResponse;
import com.unifapi.academico.dto.usuario.UsuarioUpdatePerfilRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaResponse;
import com.unifapi.academico.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ============================================
    // PERFIL DO USUÁRIO LOGADO (/me)
    // ============================================

    @GetMapping("/me")
    @Operation(summary = "Obter dados do usuário autenticado", description = "Retorna os dados do perfil do usuário logado")
    public ResponseEntity<UsuarioPerfilResponse> obterMeuPerfil(
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        UsuarioPerfilResponse perfil = usuarioService.obterPorId(usuarioLogado.getId());
        return ResponseEntity.ok(perfil);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout do usuário", description = "Revoga o token JWT do usuário logado")
    public ResponseEntity<MensagemResponse> logout(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(usuarioService.logout(token));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar perfil do usuário logado", description = "Atualiza os dados cadastrais do próprio usuário autenticado")
    public ResponseEntity<UsuarioPerfilResponse> atualizarMeuPerfil(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @Valid @RequestBody UsuarioUpdatePerfilRequest request
    ) {
        UsuarioPerfilResponse perfilAtualizado = usuarioService.atualizarPerfil(usuarioLogado.getId(), request);
        return ResponseEntity.ok(perfilAtualizado);
    }

    @PatchMapping("/me/senha")
    @Operation(summary = "Alterar senha do usuário", description = "Altera a senha do usuário logado")
    public ResponseEntity<UsuarioUpdateSenhaResponse> alterarSenha(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @Valid @RequestBody UsuarioUpdateSenhaRequest request
    ) {
        UsuarioUpdateSenhaResponse response = usuarioService.atualizarSenha(usuarioLogado.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de foto de perfil", description = "Recebe um arquivo de imagem e atualiza a foto do perfil do usuário logado")
    public ResponseEntity<UsuarioPerfilResponse> uploadFoto(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestParam("foto") MultipartFile arquivo
    ) {
        UsuarioPerfilResponse perfilAtualizado = usuarioService.atualizarFoto(usuarioLogado.getId(), arquivo);
        return ResponseEntity.ok(perfilAtualizado);
    }

    // ============================================
    // CONSULTAS E ADMINISTRAÇÃO
    // ============================================

    @PatchMapping("/{id}/nivel-acesso")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Alterar nível de acesso (Admin)", description = "Permite que um administrador promova um usuário a PROFESSOR ou ADMIN")
    public ResponseEntity<UsuarioPerfilResponse> alterarNivelAcesso(
            @PathVariable UUID id,
            @RequestParam UUID idNivel
    ) {
        UsuarioPerfilResponse usuarioAtualizado = usuarioService.alterarNivelAcesso(id, idNivel);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    @GetMapping
    @Operation(summary = "Listar todos os usuários", description = "Retorna uma lista completa de usuários")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResumoResponse>> listarUsuarios() {
        List<UsuarioResumoResponse> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/explorar")
    @Operation(
        summary = "Explorar perfis de alunos e professores", 
        description = "Retorna uma lista paginada de usuários ativos, ignorando perfis administradores."
    )
    public ResponseEntity<Page<UsuarioResumoResponse>> explorarPerfis(
            @RequestParam(name = "busca", required = false) String busca,
            @RequestParam(name = "idCurso", required = false) UUID idCurso,
            @ParameterObject @PageableDefault(size = 12, sort = "nome") Pageable pageable
    ) {
        Page<UsuarioResumoResponse> perfis = usuarioService.buscarPerfisPublicos(busca, idCurso, pageable);
        return ResponseEntity.ok(perfis);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário específico pelo seu ID")
    public ResponseEntity<UsuarioPerfilResponse> obterPorId(@PathVariable UUID id) {
        UsuarioPerfilResponse perfil = usuarioService.obterPorId(id);
        return ResponseEntity.ok(perfil);
    }
}