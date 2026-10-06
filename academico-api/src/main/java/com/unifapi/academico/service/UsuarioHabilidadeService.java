package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.NivelHabilidade;
import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.domain.models.UsuarioHabilidade;
import com.unifapi.academico.dto.habilidade.AdicionarHabilidadeUsuarioRequest;
import com.unifapi.academico.dto.habilidade.UsuarioHabilidadeResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.UsuarioHabilidadeRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Tags de competência do próprio usuário. É a tabela de junção entre Usuario
 * e o catálogo de Habilidade — não cria habilidades novas, apenas vincula
 * as existentes (ver HabilidadeService para o CRUD do catálogo).
 */
@Service
@RequiredArgsConstructor
public class UsuarioHabilidadeService {

    private final UsuarioHabilidadeRepository usuarioHabilidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final HabilidadeService habilidadeService;

    @Transactional(readOnly = true)
    public List<UsuarioHabilidadeResponse> listar(UUID usuarioId) {
        return usuarioHabilidadeRepository.findByUsuario_IdOrderByHabilidade_NomeAsc(usuarioId).stream()
                .map(UsuarioHabilidadeResponse::fromEntity)
                .toList();
    }

    @Transactional
    public UsuarioHabilidadeResponse adicionar(UUID usuarioId, AdicionarHabilidadeUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        Habilidade habilidade = habilidadeService.obterEntidade(request.habilidadeId());

        if (usuarioHabilidadeRepository.existsByUsuario_IdAndHabilidade_Id(usuarioId, habilidade.getId())) {
            throw new RegraDeNegocioException("A habilidade '" + habilidade.getNome() + "' já está no seu perfil.");
        }

        UsuarioHabilidade vinculo = usuarioHabilidadeRepository.save(UsuarioHabilidade.builder()
                .usuario(usuario)
                .habilidade(habilidade)
                .nivel(request.nivel() != null ? request.nivel() : NivelHabilidade.INICIANTE)
                .build());

        usuarioHabilidadeRepository.flush();
        habilidadeService.recalcularContadores(habilidade.getId());

        return UsuarioHabilidadeResponse.fromEntity(vinculo);
    }

    @Transactional
    public UsuarioHabilidadeResponse atualizarNivel(UUID usuarioId, UUID habilidadeId, NivelHabilidade nivel) {
        UsuarioHabilidade vinculo = obterVinculo(usuarioId, habilidadeId);
        vinculo.setNivel(nivel);
        return UsuarioHabilidadeResponse.fromEntity(usuarioHabilidadeRepository.save(vinculo));
    }

    @Transactional
    public void remover(UUID usuarioId, UUID habilidadeId) {
        UsuarioHabilidade vinculo = obterVinculo(usuarioId, habilidadeId);

        usuarioHabilidadeRepository.delete(vinculo);
        usuarioHabilidadeRepository.flush();

        habilidadeService.recalcularContadores(habilidadeId);
    }

    private UsuarioHabilidade obterVinculo(UUID usuarioId, UUID habilidadeId) {
        return usuarioHabilidadeRepository.findByUsuario_IdAndHabilidade_Id(usuarioId, habilidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esta habilidade não está no seu perfil."));
    }
}
