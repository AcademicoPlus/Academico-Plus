package com.unifapi.academico.api.controllers;

import com.unifapi.academico.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;

import com.unifapi.academico.api.dto.request.LoginRequestDTO;
import com.unifapi.academico.api.dto.request.SignupRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login - Fazer login
     */
    @PostMapping("/login")
    @Operation (summary = "Fazer login", description = "Autentica o usuário e retorna um token JWT")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            Map<String, Object> response = authService.login(request.getEmail(), request.getSenha());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/signup - Registrar novo usuário
     */
    @PostMapping("/signup")
    @Operation (summary = "Registrar novo usuário", description = "Cria um novo usuário e retorna os dados do usuário criado")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequestDTO request) {
        try {
            Map<String, Object> response = authService.signup(
                request.getEmail(),
                request.getNome(),
                request.getCurso(),
                request.getSenha()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * GET /api/auth/me - Obter dados do usuário autenticado
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Obter dados do usuário autenticado", description = "Retorna os dados do usuário autenticado com base no token JWT fornecido")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader("Authorization") String token) {
        try {
            // Remover "Bearer " do token
            String jwtToken = token.replace("Bearer ", "");
            Map<String, Object> response = authService.me(jwtToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * GET /api/auth/health - Health check
     */
    @GetMapping("/health")
    @Operation (summary = "Health check", description = "Verifica se a API está funcionando corretamente")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}