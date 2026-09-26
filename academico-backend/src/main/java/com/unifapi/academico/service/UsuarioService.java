package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.Curso;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.TokenRevogado;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.usuario.ExcluirContaRequest;
import com.unifapi.academico.dto.usuario.UsuarioPerfilResponse;
import com.unifapi.academico.dto.usuario.UsuarioUpdatePerfilRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CursoRepository;
import com.unifapi.academico.repository.NivelAcessoRepository;
import com.unifapi.academico.repository.TokenRevogadoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final NivelAcessoRepository nivelAcessoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRevogadoRepository tokenRevogadoRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final FotoStorageClient fotoStorageClient;

    // ============================================
    // CADASTRO E BUSCA
    // ============================================
    @Transactional
    public UsuarioPerfilResponse alterarNivelAcesso(UUID idUsuario, UUID idNovoNivel) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        NivelAcesso novoNivel = nivelAcessoRepository.findById(idNovoNivel)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nível de acesso não encontrado."));

        usuario.setNivelAcesso(novoNivel);

        return UsuarioPerfilResponse.fromEntity(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioPerfilResponse obterPorId(UUID id) {
        Usuario usuario = buscarUsuarioPorId(id);
        return UsuarioPerfilResponse.fromEntity(usuario);
    }

    /** Usado por GET /usuarios/{id}: omite dados privados (email, aceite de termos) quando o alvo não é quem está logado. */
    @Transactional(readOnly = true)
    public UsuarioPerfilResponse obterPorId(UUID id, UUID idUsuarioLogado) {
        Usuario usuario = buscarUsuarioPorId(id);
        boolean ehProprioPerfil = usuario.getId().equals(idUsuarioLogado);
        return UsuarioPerfilResponse.fromEntity(usuario, ehProprioPerfil);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResumoResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResumoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResumoResponse> buscarPerfisPublicos(String busca, UUID idCurso, UUID idHabilidade, Pageable pageable) {
        Specification<Usuario> spec = Specification
                .where(UsuarioRepository.somenteAtivos())
                .and(UsuarioRepository.semAdmin())
                .and(UsuarioRepository.comCurso(idCurso))
                .and(UsuarioRepository.comHabilidade(idHabilidade))
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

    @Transactional
    public MensagemResponse logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            if (!tokenRevogadoRepository.existsByToken(token)) {
                // Extrai a data de expiração contida nas claims do JWT
                LocalDateTime dataExpiracao = jwtTokenProvider.obterDataExpiracaoDoToken(token);

                TokenRevogado tokenRevogado = new TokenRevogado(
                        token,
                        LocalDateTime.now(),
                        dataExpiracao
                );
                tokenRevogadoRepository.save(tokenRevogado);
            }
        }
        return new MensagemResponse(true, "Logout realizado com sucesso.");
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

        if (request.periodo() != null)
            usuario.setPeriodo(request.periodo());
        if (request.bio() != null)
            usuario.setBio(request.bio());
        if (request.fotoUrl() != null)
            usuario.setFotoUrl(request.fotoUrl());
        if (request.linkedinUrl() != null)
            usuario.setLinkedinUrl(request.linkedinUrl());
        if (request.githubUrl() != null)
            usuario.setGithubUrl(request.githubUrl());

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

    /** Envia a nova foto pro academico-upload-service e remove a anterior, se existir. */
    @Transactional
    public UsuarioPerfilResponse atualizarFoto(UUID idUsuario, MultipartFile arquivo) {
        Usuario usuario = buscarUsuarioPorId(idUsuario);

        String fotoAntiga = usuario.getFotoUrl();
        usuario.setFotoUrl(fotoStorageClient.salvar(arquivo));

        if (fotoAntiga != null && !fotoAntiga.isBlank()) {
            fotoStorageClient.deletar(fotoAntiga);
        }

        return UsuarioPerfilResponse.fromEntity(usuario);
    }

    /** Remove a foto de perfil atual, se existir. */
    @Transactional
    public UsuarioPerfilResponse removerFoto(UUID idUsuario) {
        Usuario usuario = buscarUsuarioPorId(idUsuario);

        String fotoAtual = usuario.getFotoUrl();
        if (fotoAtual == null || fotoAtual.isBlank()) {
            throw new RegraDeNegocioException("Você não possui foto de perfil para remover.");
        }

        usuario.setFotoUrl(null);
        fotoStorageClient.deletar(fotoAtual);

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

    /**
     * Exclusão da própria conta (LGPD: direito do titular de solicitar a
     * exclusão dos seus dados). Exige a senha atual — sem isso, uma sessão
     * sequestrada poderia apagar a conta sem o dono perceber. Depois de
     * ativo=false, o @SQLRestriction da entidade já barra o token atual em
     * qualquer requisição futura, então não é preciso revogar o JWT à parte.
     */
    @Transactional
    public void excluirMinhaConta(UUID idUsuario, ExcluirContaRequest request) {
        Usuario usuario = buscarUsuarioPorId(idUsuario);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("Senha incorreta.");
        }

        usuario.setAtivo(false);
        usuario.setDeletadoEm(LocalDateTime.now());
    }

    // ============================================
    // MÉTODOS AUXILIARES (HELPERS)
    // ============================================

    private Usuario buscarUsuarioPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID: " + id));
    }

    private Curso resolverCurso(UUID idCurso) {
        return cursoRepository.findById(idCurso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado com o ID: " + idCurso));
    }
}