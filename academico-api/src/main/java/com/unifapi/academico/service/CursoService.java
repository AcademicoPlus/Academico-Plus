package com.unifapi.academico.service;

import com.unifapi.academico.dto.usuario.CursoResponse;
import com.unifapi.academico.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepository;

    @Transactional(readOnly = true)
    public List<CursoResponse> listarAtivos() {
        return cursoRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(CursoResponse::fromEntity)
                .toList();
    }
}
