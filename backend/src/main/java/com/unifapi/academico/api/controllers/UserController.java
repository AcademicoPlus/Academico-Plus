package com.unifapi.academico.api.controllers;

import com.unifapi.academico.api.dto.request.CreateUserRequest;
import com.unifapi.academico.api.dto.request.UpdateUserDTO;
import com.unifapi.academico.api.dto.response.UserDTO;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor 
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/type/{type}")
    @Operation(summary = "Listar usuários por tipo", description = "Retorna usuários filtrados por tipo (STUDENT ou PROFESSOR)")
    public ResponseEntity<List<UserDTO>> getUsersByType(@PathVariable TipoUsuario type) { // O Spring converte String para Enum automaticamente
        List<UserDTO> users = userService.getUsersByType(type);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping
    @Operation(summary = "Listar todos os usuários", description = "Retorna uma lista de todos os usuários cadastrados")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário específico pelo seu ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar novo usuário (Admin)", description = "Permite que administradores criem usuários com tipos específicos")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza dados de um usuário existente")
    public ResponseEntity<UserDTO> updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserDTO request
    ) {
        UserDTO user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar usuário", description = "Remove um usuário do sistema")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}