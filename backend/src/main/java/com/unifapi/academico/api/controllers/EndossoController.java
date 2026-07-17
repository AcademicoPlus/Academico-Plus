package com.unifapi.academico.api.controllers;

import com.unifapi.academico.domain.model.EndossoProfessor;
import com.unifapi.academico.repository.EndossoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.repository.HabilidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EndossoController {

    private final EndossoRepository endossoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HabilidadeRepository habilidadeRepository;

    /**
     * POST /api/usuarios/{alunoId}/habilidades/{habilidadeId}/endossar
     * Professor endossa uma habilidade de um aluno
     */
    @PostMapping("/{alunoId}/habilidades/{habilidadeId}/endossar")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation (summary = "Endossar habilidade de aluno", description = "Permite que um professor endosse uma habilidade específica de um aluno.")
    public ResponseEntity<?> endossar(
            @PathVariable Long alunoId,
            @PathVariable Long habilidadeId) {
        
        try {
            
            var aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));

            
            var habilidade = habilidadeRepository.findById(habilidadeId)
                .orElseThrow(() -> new RuntimeException("Habilidade não encontrada"));

            
            var professor = usuarioRepository.findById(10L)
                .orElseThrow(() -> new RuntimeException("Professor não encontrado"));

            
            var endossoExistente = endossoRepository
                .findByProfessorIdAndAlunoIdAndHabilidadeId(
                    professor.getId(),
                    alunoId,
                    habilidadeId
                );

            if (endossoExistente.isPresent()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("erro", "Endosso já existe para esta combinação"));
            }

            // Criar novo endosso
            EndossoProfessor endosso = EndossoProfessor.builder()
                .professor(professor)
                .aluno(aluno)
                .habilidade(habilidade)
                .dataEndosso(LocalDateTime.now())
                .build();

            EndossoProfessor salvo = endossoRepository.save(endosso);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "id", salvo.getId(),
                    "alunoId", salvo.getAluno().getId(),
                    "habilidadeId", salvo.getHabilidade().getId(),
                    "professorId", salvo.getProfessor().getId(),
                    "dataEndosso", salvo.getDataEndosso()
                ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("erro", e.getMessage()));
        }
    }
}