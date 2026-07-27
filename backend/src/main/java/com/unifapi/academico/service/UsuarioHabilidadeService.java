package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.domain.model.Habilidade;
import com.unifapi.academico.domain.model.UsuarioHabilidade;
import com.unifapi.academico.domain.enums.NivelHabilidade;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.repository.HabilidadeRepository;
import com.unifapi.academico.repository.UsuarioHabilidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioHabilidadeService {

    private final UsuarioHabilidadeRepository usuarioHabilidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final HabilidadeRepository habilidadeRepository;

    /**
     * Adicionar habilidade a um usuário
     */
    public UsuarioHabilidade adicionarHabilidade(UUID usuarioId, UUID habilidadeId, NivelHabilidade nivel) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Habilidade habilidade = habilidadeRepository.findById(habilidadeId)
            .orElseThrow(() -> new RuntimeException("Habilidade não encontrada"));

        // Verificar se usuário já tem essa habilidade
        Optional<UsuarioHabilidade> existente = usuarioHabilidadeRepository
            .findByUsuarioIdAndHabilidadeId(usuarioId, habilidadeId);

        if (existente.isPresent()) {
            // Se já existe, apenas atualizar o nível
            UsuarioHabilidade uh = existente.get();
            uh.setNivel(nivel);
            return usuarioHabilidadeRepository.save(uh);
        }

        // Se não existe, criar nova
        UsuarioHabilidade usuarioHabilidade = UsuarioHabilidade.builder()
            .usuario(usuario)
            .habilidade(habilidade)
            .nivel(nivel)
            .endossado(false)
            .endossosCount(0)
            .build();

        return usuarioHabilidadeRepository.save(usuarioHabilidade);
    }

    /**
     * Remover habilidade de um usuário
     */
    public void removerHabilidade(UUID usuarioId, UUID habilidadeId) {
        UsuarioHabilidade usuarioHabilidade = usuarioHabilidadeRepository
            .findByUsuarioIdAndHabilidadeId(usuarioId, habilidadeId)
            .orElseThrow(() -> new RuntimeException("Habilidade não encontrada para este usuário"));

        usuarioHabilidadeRepository.delete(usuarioHabilidade);
    }

    /**
     * Listar habilidades de um usuário
     */
    public List<UsuarioHabilidade> listarHabilidades(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return usuarioHabilidadeRepository.findByUsuarioId(usuarioId);
    }

    /**
     * Obter uma habilidade específica de um usuário
     */
    public Optional<UsuarioHabilidade> obterHabilidade(UUID usuarioId, UUID habilidadeId) {
        return usuarioHabilidadeRepository.findByUsuarioIdAndHabilidadeId(usuarioId, habilidadeId);
    }

    /**
     * Atualizar nível de uma habilidade
     */
    public UsuarioHabilidade atualizarNivel(UUID usuarioId, UUID habilidadeId, NivelHabilidade novoNivel) {
        UsuarioHabilidade usuarioHabilidade = usuarioHabilidadeRepository
            .findByUsuarioIdAndHabilidadeId(usuarioId, habilidadeId)
            .orElseThrow(() -> new RuntimeException("Habilidade não encontrada para este usuário"));

        usuarioHabilidade.setNivel(novoNivel);
        return usuarioHabilidadeRepository.save(usuarioHabilidade);
    }
}