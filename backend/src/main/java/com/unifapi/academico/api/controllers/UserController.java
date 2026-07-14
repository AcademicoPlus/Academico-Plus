package com.unifapi.academico.api.controllers;

import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.service.UserService;

import io.swagger.v3.oas.annotations.Operation;

import com.unifapi.academico.api.dto.response.UserDTO;
import com.unifapi.academico.api.dto.request.CreateUserRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    // 1. ROTAS ESPECÍFICAS PRIMEIRO (com paths fixos)
    @GetMapping("/type/{type}")
    @Operation(summary = "Listar usuários por tipo", description = "Retorna usuários filtrados por tipo (STUDENT ou PROFESSOR)")
    public ResponseEntity<List<UserDTO>> getUsersByType(@PathVariable String type) {
        TipoUsuario userType = TipoUsuario.valueOf(type.toUpperCase());
        List<UserDTO> users = userService.getUsersByType(userType);
        return ResponseEntity.ok(users);
    }
    
    // 2. ROTAS GENÉRICAS DEPOIS (com {id})
    @GetMapping
    @Operation(summary = "Listar todos os usuários", description = "Retorna uma lista de todos os usuários cadastrados")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário específico pelo seu ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    @Operation(summary = "Criar novo usuário", description = "Cria um novo usuário no sistema")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza dados de um usuário existente")
    public ResponseEntity<UserDTO> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody CreateUserRequest request
    ) {
        UserDTO user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar usuário", description = "Remove um usuário do sistema")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}