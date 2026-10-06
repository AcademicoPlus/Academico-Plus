package com.unifapi.academico.controller;

import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.avaliacao.AvaliacaoResponse;
import com.unifapi.academico.dto.avaliacao.AvaliarRequest;
import com.unifapi.academico.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/projetos/{projetoId}/avaliacoes")
@RequiredArgsConstructor
public class ProjetoAvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping
    @Operation(summary = "Avaliar um participante do projeto",
            description = "Só é permitido entre quem participou do mesmo projeto (criador ou membro), "
                    + "depois que o projeto está CONCLUIDO ou CANCELADO")
    public ResponseEntity<AvaliacaoResponse> avaliar(
            @PathVariable UUID projetoId,
            @Valid @RequestBody AvaliarRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        AvaliacaoResponse avaliacao = avaliacaoService.avaliar(projetoId, request, usuarioLogado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(avaliacao);
    }
}
