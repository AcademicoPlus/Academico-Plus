package com.unifapi.academico.controller;


import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.auth.ResetPreviewRequest;
import com.unifapi.academico.dto.auth.ResetPreviewResponse;
import com.unifapi.academico.dto.auth.ResetRedefinirRequest;
import com.unifapi.academico.dto.auth.ResetSolicitarRequest;
import com.unifapi.academico.dto.auth.ResetValidarRequest;
import com.unifapi.academico.dto.auth.ResetValidarResponse;
import com.unifapi.academico.dto.auth.CadastroRequest;
import com.unifapi.academico.dto.auth.CadastroResponse;
import com.unifapi.academico.dto.auth.LoginRequest;
import com.unifapi.academico.dto.auth.LoginResponse;

import com.unifapi.academico.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login de usuário", description = "Autentica um usuário e retorna um token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/cadastro")
    @Operation(summary = "Cadastro de usuário", description = "Cadastra um novo usuário no sistema")
    public ResponseEntity<CadastroResponse> cadastro(@Valid @RequestBody CadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.cadastrar(request));
    }

    @PostMapping("/reset-senha/solicitar")
    @Operation(summary = "Solicitar redefinição de senha", description = "Solicita a redefinição de senha para o e-mail fornecido")
    public ResponseEntity<MensagemResponse> resetSolicitar(@Valid @RequestBody ResetSolicitarRequest request) {
        return ResponseEntity.ok(authService.solicitarResetSenha( request.email()));
    }

    @PostMapping("/reset-senha/preview")
    @Operation(summary = "Pré-visualizar redefinição de senha", description = "Pré-visualiza o processo de redefinição de senha para o e-mail fornecido")
    public ResponseEntity<ResetPreviewResponse> resetPreview(@Valid @RequestBody ResetPreviewRequest request) {
        return ResponseEntity.ok(authService.previewResetTarget(request.email()));
    }

    @PostMapping("/reset-senha/validar")
    @Operation(summary = "Validar código de redefinição de senha", description = "Valida o código enviado para o e-mail do usuário para redefinição de senha")
    public ResponseEntity<ResetValidarResponse> resetValidar(@Valid @RequestBody ResetValidarRequest request) {
        return ResponseEntity.ok(authService.validarOtp(request.email(), request.codigo()));
    }

    @PostMapping("/reset-senha/redefinir")
    @Operation(summary = "Redefinir senha", description = "Redefine a senha do usuário usando o token de redefinição")
    public ResponseEntity<MensagemResponse> resetRedefinir(@Valid @RequestBody ResetRedefinirRequest request) {
        return ResponseEntity.ok(authService.redefinirSenha(request.tokenRedefinicao(), request.novaSenha()));
    }
}