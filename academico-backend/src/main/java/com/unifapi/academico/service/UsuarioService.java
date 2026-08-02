package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.Curso;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.CadastroRequest;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.usuario.UsuarioPerfilResponse;
import com.unifapi.academico.dto.usuario.UsuarioUpdatePerfilRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CursoRepository;
import com.unifapi.academico.repository.NivelAcessoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final NivelAcessoRepository nivelAcessoRepository;
    private final PasswordEncoder passwordEncoder;

    // ============================================
    // CADASTRO E BUSCA
    // ============================================

    @Transactional
    public UsuarioPerfilResponse criarUsuario(CadastroRequest request) {
        String emailNorm = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(emailNorm)) {
            throw new RegraDeNegocioException("E-mail '" + emailNorm + "' já está cadastrado.");
        }

        NivelAcesso nivel = request.idNivel() != null 
                ? resolverNivelAcesso(request.idNivel())
                : resolverNivelAcessoPorNome("ALUNO");

        Curso curso = request.idCurso() != null ? resolverCurso(request.idCurso()) : null;

        Usuario usuario = Usuario.builder()
                .email(emailNorm)
                .nome(request.nome().trim())
                .nivelAcesso(nivel)
                .curso(curso)
                .periodo(request.periodo())
                .senhaHash(passwordEncoder.encode(request.senha()))
                .ativo(true)
                .build();

        usuario = usuarioRepository.save(usuario);
        return UsuarioPerfilResponse.fromEntity(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioPerfilResponse obterPorId(UUID id) {
        Usuario usuario = buscarUsuarioPorId(id);
        return UsuarioPerfilResponse.fromEntity(usuario);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResumoResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResumoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResumoResponse> buscarPerfisPublicos(String busca, UUID idCurso, Pageable pageable) {
        Specification<Usuario> spec = Specification
                .where(UsuarioRepository.somenteAtivos())
                .and(UsuarioRepository.semAdmin())
                .and(UsuarioRepository.comCurso(idCurso))
                .and(UsuarioRepository.comBuscaTextual(busca))
                .and(UsuarioRepository.comFetchRelacoes());

        return usuarioRepository.findAll(spec, pageable)
                .map(UsuarioResumoResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioPerfilResponse> listarPaginado(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(UsuarioPerfilResponse::fromEntity);
    }

    // ============================================
    // ATUALIZAÇÃO DE PERFIL
    // ============================================

    @Transactional
    public UsuarioPerfilResponse atualizarPerfil(UUID idUsuario, UsuarioUpdatePerfilRequest request) {
        Usuario usuario = buscarUsuarioPorId(idUsuario);

        if (request.idCurso() != null) {
            Curso curso = resolverCurso(request.idCurso());
            usuario.setCurso(curso);
        }

        if (request.nome() != null && !request.nome().isBlank()) {
            usuario.setNome(request.nome().trim());
        }

        if (request.periodo() != null) usuario.setPeriodo(request.periodo());
        if (request.bio() != null) usuario.setBio(request.bio());
        if (request.fotoUrl() != null) usuario.setFotoUrl(request.fotoUrl());
        if (request.linkedinUrl() != null) usuario.setLinkedinUrl(request.linkedinUrl());
        if (request.githubUrl() != null) usuario.setGithubUrl(request.githubUrl());

        return UsuarioPerfilResponse.fromEntity(usuario);
    }

    @Transactional
    public UsuarioUpdateSenhaResponse atualizarSenha(UUID idUsuario, UsuarioUpdateSenhaRequest request) {
        Usuario usuario = buscarUsuarioPorId(idUsuario);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("Senha atual incorreta.");
        }

        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));

        return UsuarioUpdateSenhaResponse.sucesso();
    }

    @Transactional
    public UsuarioPerfilResponse atualizarFoto(UUID idUsuario, String fotoUrl) {
        Usuario usuario = buscarUsuarioPorId(idUsuario);
        usuario.setFotoUrl(fotoUrl);
        return UsuarioPerfilResponse.fromEntity(usuario);
    }

    // ============================================
    // SOFT DELETE E RESTAURAÇÃO
    // ============================================

    @Transactional
    public void deletarUsuario(UUID idParaDeletar, UUID idUsuarioLogado) {
        Usuario usuario = buscarUsuarioPorId(idParaDeletar);

        if (usuario.getId().equals(idUsuarioLogado)) {
            throw new RegraDeNegocioException("Você não pode inativar sua própria conta.");
        }

        if (usuario.getNivelAcesso() != null && "ADMIN".equalsIgnoreCase(usuario.getNivelAcesso().getNome())) {
            throw new RegraDeNegocioException("Não é permitido inativar administradores.");
        }

        // Soft delete real
        usuario.setAtivo(false);
    }


    @Transactional
    public void reativarUsuario(UUID idParaReativar) {
        Usuario usuario = buscarUsuarioPorId(idParaReativar);
        usuario.setAtivo(true);
    }

    // ============================================
    // MÉTODOS AUXILIARES (HELPERS)
    // ============================================

    private Usuario buscarUsuarioPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID: " + id));
    }

    private NivelAcesso resolverNivelAcesso(UUID idNivel) {
        return nivelAcessoRepository.findById(idNivel)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nível de acesso não encontrado com o ID: " + idNivel));
    }

    private NivelAcesso resolverNivelAcessoPorNome(String nome) {
        return nivelAcessoRepository.findByNome(nome)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nível de acesso '" + nome + "' não está configurado no banco."));
    }

    private Curso resolverCurso(UUID idCurso) {
        return cursoRepository.findById(idCurso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado com o ID: " + idCurso));
    }
}