package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.usuario.UsuarioPerfilResponse;
import com.unifapi.academico.dto.usuario.UsuarioUpdatePerfilRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaRequest;
import com.unifapi.academico.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    // Injecte um serviço de upload (S3, Cloudinary ou armazenamento local) se houver
    // private final StorageService storageService;

    /**
     * Retorna os dados do perfil do usuário atualmente autenticado.
     */
    @GetMapping("/me")
    @Operation (summary = "Obter dados do usuário autenticado", description = "Retorna os dados do usuário autenticado")
    public ResponseEntity<UsuarioPerfilResponse> obterMeuPerfil(
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        UsuarioPerfilResponse perfil = usuarioService.obterPorId(usuarioLogado.getId());
        return ResponseEntity.ok(perfil);
    }

    @GetMapping("/listar-todos")
    @Operation(summary = "Listar todos os usuários", description = "Retorna uma lista de todos os usuários")
    public ResponseEntity<List<UsuarioResumoResponse>> listarUsuarios() {
        List<UsuarioResumoResponse> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Retorna os dados do perfil de qualquer usuário por ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário específico pelo seu ID")
    public ResponseEntity<UsuarioPerfilResponse> obterPorId(@PathVariable UUID id) {
        UsuarioPerfilResponse perfil = usuarioService.obterPorId(id);
        return ResponseEntity.ok(perfil);
    }

    /**
     * Atualiza os dados básicos do perfil do usuário logado.
     */
    @PutMapping("/atualizar-perfil")
    @Operation(summary = "Atualizar usuário", description = "Atualiza dados de um usuário existente")
    public ResponseEntity<UsuarioPerfilResponse> atualizarPerfil(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @Valid @RequestBody UsuarioUpdatePerfilRequest request
    ) {
        UsuarioPerfilResponse perfilAtualizado = usuarioService.atualizarPerfil(usuarioLogado.getId(), request);
        return ResponseEntity.ok(perfilAtualizado);
    }

    /**
     * Altera a senha do usuário logado.
     */
    @PatchMapping("/me/senha")
    @Operation(summary = "Alterar senha do usuário", description = "Altera a senha do usuário logado")
    public ResponseEntity<Void> alterarSenha(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @Valid @RequestBody UsuarioUpdateSenhaRequest request
    ) {
        usuarioService.atualizarSenha(usuarioLogado.getId(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Recebe um arquivo de imagem (MultipartFile), salva no armazenamento e atualiza a foto do perfil.
     */
    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de foto de perfil", description = "Recebe um arquivo de imagem e atualiza a foto do perfil do usuário logado")
    public ResponseEntity<UsuarioPerfilResponse> uploadFoto(
            @AuthenticationPrincipal Usuario usuarioLogado,
            @RequestParam("foto") MultipartFile arquivo
    ) {
        // Exemplo com serviço de upload local/cloud:
        // String urlFoto = storageService.salvar(arquivo);
        
        // Simulação com URL gerada/recebida:
        String urlFoto = "https://storage.unifapi.edu.br/fotos/" + arquivo.getOriginalFilename();

        UsuarioPerfilResponse perfilAtualizado = usuarioService.atualizarFoto(usuarioLogado.getId(), urlFoto);
        return ResponseEntity.ok(perfilAtualizado);
    }
}