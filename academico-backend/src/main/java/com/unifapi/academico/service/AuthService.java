package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.Curso;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.ProjetoCanceladoAviso;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.CadastroRequest;
import com.unifapi.academico.dto.auth.CadastroResponse;
import com.unifapi.academico.dto.auth.LoginRequest;
import com.unifapi.academico.dto.auth.LoginResponse;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.auth.ResetPreviewResponse;
import com.unifapi.academico.dto.auth.ResetValidarResponse;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.projeto.ProjetoCanceladoResumoResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CursoRepository;
import com.unifapi.academico.repository.NivelAcessoRepository;
import com.unifapi.academico.repository.ProjetoCanceladoAvisoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final NivelAcessoRepository nivelAcessoRepository;
    private final ProjetoCanceladoAvisoRepository projetoAvisoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    // ============================================
    // LOGIN & CADASTRO PÚBLICO
    // ============================================

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return new LoginResponse(false, "Informe o e-mail.", null, null, List.of());
        }

        String emailNorm = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(emailNorm).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            return new LoginResponse(false, "Email ou senha incorretos.", null, null, List.of());
        }

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new RegraDeNegocioException("Esta conta está desativada.");
        }

        String token = jwtTokenProvider.gerarTokenAcesso(usuario.getId(), usuario.getNivelAcesso().getNome());
        UsuarioResumoResponse resumo = UsuarioResumoResponse.fromEntity(usuario);
        
        List<ProjetoCanceladoResumoResponse> projetosCancelados = consumirProjetosCancelados(usuario.getId());

        return new LoginResponse(true, "Login realizado com sucesso.", token, resumo, projetosCancelados);
    }

    @Transactional
    public CadastroResponse cadastrar(CadastroRequest request) {
        String emailNorm = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(emailNorm)) {
            throw new RegraDeNegocioException("E-mail '" + emailNorm + "' já está cadastrado no sistema.");
        }

        Curso curso = cursoRepository.findById(request.idCurso())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado com o ID: " + request.idCurso()));

        NivelAcesso nivelAcesso = nivelAcessoRepository.findByNome("ALUNO")
            .orElseThrow(() -> new RecursoNaoEncontradoException("Nível de acesso padrão não encontrado."));
        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(emailNorm)
                .senhaHash(passwordEncoder.encode(request.senha()))
                .curso(curso)
                .periodo(request.periodo())
                .nivelAcesso(nivelAcesso)
                .ativo(true)
                .build();

        usuarioRepository.save(usuario);
        return new CadastroResponse(true, "Cadastro realizado com sucesso.");
    }

    public MensagemResponse logout(){
        return new MensagemResponse(true, "Logout realizado com sucesso.");
    }

    // ============================================
    // RECUPERAÇÃO DE SENHA (OTP)
    // ============================================

    /**
     * Retorna sempre a mesma mensagem de sucesso, exista ou não o e-mail —
     * caso contrário, o endpoint vira um oráculo de enumeração de usuários.
     */
    @Transactional
    public MensagemResponse solicitarResetSenha(String email) {
        if (email == null || email.isBlank()) {
            throw new RegraDeNegocioException("Informe o e-mail.");
        }

        String emailNorm = email.trim().toLowerCase();
        String mensagemGenerica = "Se este e-mail estiver cadastrado, um código de verificação foi enviado.";

        Usuario usuario = usuarioRepository.findByEmail(emailNorm).orElse(null);
        if (usuario == null) {
            return new MensagemResponse(true, mensagemGenerica);
        }

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        usuario.setCodigoRecuperacao(passwordEncoder.encode(otp));
        usuario.setCodigoRecuperacaoExpiraEm(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);

        boolean enviado = emailService.enviarCodigoVerificacao(usuario.getEmail(), otp);
        if (!enviado) {
            throw new RegraDeNegocioException("Não foi possível enviar o e-mail de recuperação. Tente novamente mais tarde.");
        }

        return new MensagemResponse(true, mensagemGenerica);
    }

    /** Só expõe a versão mascarada do e-mail: o valor completo nunca deve voltar na resposta. */
    @Transactional(readOnly = true)
    public ResetPreviewResponse previewResetTarget(String email) {
        String emailNorm = email.trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(emailNorm)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum usuário encontrado para este e-mail."));

        return new ResetPreviewResponse(true, "Usuário encontrado para recuperação.",
                mascararEmail(usuario.getEmail()));
    }

    @Transactional
    public ResetValidarResponse validarOtp(String email, String codigo) {
        String emailNorm = email.trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(emailNorm)
                .orElseThrow(() -> new RegraDeNegocioException("Código inválido ou expirado."));

        if (usuario.getCodigoRecuperacao() == null || usuario.getCodigoRecuperacaoExpiraEm() == null) {
            throw new RegraDeNegocioException("Código inválido ou expirado.");
        }

        if (usuario.getCodigoRecuperacaoExpiraEm().isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Código expirado. Solicite um novo.");
        }

        if (!passwordEncoder.matches(codigo, usuario.getCodigoRecuperacao())) {
            throw new RegraDeNegocioException("Código inválido.");
        }

        usuario.setCodigoRecuperacao(null);
        usuario.setCodigoRecuperacaoExpiraEm(null);

        String tokenRedefinicao = jwtTokenProvider.gerarTokenResetSenha(usuario.getId());

        return new ResetValidarResponse(true, tokenRedefinicao);
    }

    @Transactional
    public MensagemResponse redefinirSenha(String tokenRedefinicao, String novaSenha) {
        if (!jwtTokenProvider.validarTokenReset(tokenRedefinicao)) {
            throw new RegraDeNegocioException("Token de redefinição inválido ou expirado.");
        }

        UUID userId = jwtTokenProvider.obterSubjectDoToken(tokenRedefinicao);
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (passwordEncoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        return new MensagemResponse(true, "Senha redefinida com sucesso.");
    }

    // ============================================
    // MÉTODOS PRIVADOS / HELPERS
    // ============================================

    private List<ProjetoCanceladoResumoResponse> consumirProjetosCancelados(UUID usuarioId) {
        List<ProjetoCanceladoAviso> avisos = projetoAvisoRepository
                .findByUsuario_IdAndVisualizadoEmIsNullOrderByCriadoEmAsc(usuarioId);

        if (avisos.isEmpty()) return List.of();

        LocalDateTime agora = LocalDateTime.now();
        return avisos.stream().map(aviso -> {
            aviso.setVisualizadoEm(agora);
            return new ProjetoCanceladoResumoResponse(
                    aviso.getProjetoId(),
                    aviso.getProjetoNome(),
                    aviso.getProjetoData(),
                    aviso.getTipo()
            );
        }).toList();
    }

    private String mascararEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] partes = email.split("@", 2);
        String local = partes[0];
        String domain = partes[1];

        String localMascarado = local.length() <= 2
                ? local.charAt(0) + "*"
                : local.substring(0, 2) + "*".repeat(Math.max(2, local.length() - 2));

        return localMascarado + "@" + domain;
    }
}