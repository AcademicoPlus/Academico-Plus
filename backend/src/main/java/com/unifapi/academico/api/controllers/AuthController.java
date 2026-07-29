package com.unifapi.academico.api.controllers;

import com.unifapi.academico.api.dto.request.LoginRequestDTO;
import com.unifapi.academico.api.dto.request.RefreshTokenRequestDTO;
import com.unifapi.academico.api.dto.request.RegistrarUsuarioDTO;
import com.unifapi.academico.api.dto.response.AuthResponseDTO; 
import com.unifapi.academico.api.dto.response.UserDTO;
import com.unifapi.academico.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID; 

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService; 

    @PostMapping("/login")
    @Operation(summary = "Fazer login", description = "Autentica o usuário e retorna um token JWT")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request.getEmail(), request.getSenha());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar Access Token", description = "Recebe o Refresh Token e gera um novo Access Token e Refresh Token")
    public ResponseEntity<AuthResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout/{usuarioId}")
    @Operation(summary = "Fazer logout", description = "Invalida o Refresh Token no banco e adiciona o Access Token na blacklist")
    public ResponseEntity<Void> logout(
            @PathVariable UUID usuarioId, 
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        authService.logout(usuarioId, token); 
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signup")
    @Operation(summary = "Registrar novo usuário", description = "Cria um novo usuário e retorna os dados de autenticação")
    public ResponseEntity<AuthResponseDTO> signup(@Valid @RequestBody RegistrarUsuarioDTO request) {
        AuthResponseDTO response = authService.signup(
                request.getEmail(),
                request.getNome(),
                request.getCurso(),
                request.getSenha());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obter dados do usuário autenticado", description = "Retorna os dados do usuário autenticado com base no token JWT fornecido")
    public ResponseEntity<UserDTO> me(@RequestHeader(value = "Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        UserDTO user = authService.me(jwtToken);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se a API está funcionando corretamente")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}