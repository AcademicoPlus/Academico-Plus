package com.unifapi.academico.service;

import com.unifapi.academico.dto.usuario.NivelAcessoResponse;
import com.unifapi.academico.repository.NivelAcessoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NivelAcessoService {

    private final NivelAcessoRepository nivelAcessoRepository;

    @Transactional(readOnly = true)
    public List<NivelAcessoResponse> listarTodos() {
        return nivelAcessoRepository.findAll().stream()
                .map(NivelAcessoResponse::fromEntity)
                .sorted(Comparator.comparing(NivelAcessoResponse::nome))
                .toList();
    }
}
