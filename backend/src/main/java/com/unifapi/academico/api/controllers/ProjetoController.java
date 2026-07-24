package com.unifapi.academico.api.controllers;

import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.service.ProjetoService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

import com.unifapi.academico.api.dto.request.ProjetoRequestDTO;
import com.unifapi.academico.api.dto.response.ProjetoDetailResponseDTO;
import com.unifapi.academico.api.dto.response.ProjetoCardDTO;
import com.unifapi.academico.api.dto.response.ProjetoPageResponseDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projetos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjetoController {

    private final ProjetoService projetoService;

    private final UsuarioRepository usuarioRepository;

    /**
     * GET /api/projetos - Listar com filtros
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar projetos", description = "Retorna uma lista paginada de projetos, podendo filtrar por status")
    public ResponseEntity<ProjetoPageResponseDTO> listar(
            @RequestParam(required = false) StatusProjeto status,
            @ParameterObject Pageable pageable) {
        
        ProjetoPageResponseDTO response;
        
        if (status != null) {
            response = projetoService.findByStatusWithDetails(status, pageable);
        } else {
            response = projetoService.findAllWithDetails(pageable);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/projetos/{id} - Detalhes
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Obter detalhes do projeto", description = "Retorna os detalhes de um projeto específico pelo seu ID")
    public ResponseEntity<ProjetoDetailResponseDTO> obter(@PathVariable Long id) {
        return projetoService.findByIdDetail(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/projetos - Criar
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Criar novo projeto", description = "Cria um novo projeto no sistema")
    public ResponseEntity<ProjetoCardDTO> criar (
        @RequestBody @Valid ProjetoRequestDTO dto,
        Authentication authentication) {

            String email = authentication.getName();
            Usuario criador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            Projeto projeto = Projeto.builder()
            .titulo(dto.getTitulo())
            .descricao(dto.getDescricao())
            .vagas(dto.getVagas())
            .dataFim(dto.getDataFim())
            .criador(criador)
            .status(StatusProjeto.ABERTO)
            .vagasPreenchidas(0)
            .ativo(true)
            .build();

        // 3. Salva e retorna o DTO de resposta
        Projeto criado = projetoService.create(projeto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjetoCardDTO.from(criado));
    }
    

    /**
     * PUT /api/projetos/{id} - Atualizar
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Atualizar projeto", description = "Atualiza os dados de um projeto existente")
    public ResponseEntity<Projeto> atualizar(
            @PathVariable Long id,
            @RequestBody Projeto projeto) {
        try {
            Projeto atualizado = projetoService.update(id, projeto);
            return ResponseEntity.ok(atualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/projetos/{id} - Deletar
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation (summary = "Deletar projeto", description = "Remove um projeto do sistema")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        projetoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}