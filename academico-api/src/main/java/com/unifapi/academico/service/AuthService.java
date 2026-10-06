package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.Curso;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.ProjetoCanceladoAviso;
import com.unifapi.academico.domain.models.TokenRevogado;
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
import com.unifapi.academico.repository.TokenRevogadoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.security.JwtTokenProvider;
import com.unifapi.academico.security.RateLimiterService;
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
    private final TokenRevogadoRepository tokenRevogadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CodigoVerificacaoPublisher codigoVerificacaoPublisher;
    private final RateLimiterService rateLimiterService;

    // ============================================
    // LOGIN & CADASTRO PÚBLICO
    // ============================================

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return new LoginResponse(false, "Informe o e-mail.", null, null, List.of());
        }

        String emailNorm = request.email().trim().toLowerCase();

        rateLimiterService.verificarDisponivel(emailNorm);

        Usuario usuario = usuarioRepository.findByEmail(emailNorm).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            rateLimiterService.registrarFalha(emailNorm);
            return new LoginResponse(false, "Email ou senha incorretos.", null, null, List.of());
        }

        rateLimiterService.registrarSucesso(emailNorm);

        // Mesmo contrato de resposta (200 + sucesso:false) da credencial errada acima,
        // em vez de lançar exceção — mantém as duas falhas de login consistentes.
        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            return new LoginResponse(false, "Esta conta está desativada.", null, null, List.of());
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

        // Fire-and-forget: o envio passou a ser assíncrono (fila consumida pelo
        // academico-email-service). Falhas reais de envio não voltam pro chamador
        // HTTP — ficam observáveis via log/DLQ do próprio email-service.
        codigoVerificacaoPublisher.publicar(usuario.getEmail(), otp, 15);

        return new MensagemResponse(true, mensagemGenerica);
    }

    /**
     * Só expõe a versão mascarada do e-mail: o valor completo nunca deve voltar na resposta.
     * Nunca lança 404 pra e-mail não cadastrado — mesma regra do solicitarResetSenha: revelar
     * a existência da conta aqui destruiria a proteção anti-enumeração daquele endpoint, já que
     * o front chama os dois em sequência no mesmo passo do fluxo.
     */
    @Transactional(readOnly = true)
    public ResetPreviewResponse previewResetTarget(String email) {
        String emailNorm = email.trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(emailNorm).orElse(null);
        String emailParaMascarar = usuario != null ? usuario.getEmail() : emailNorm;

        return new ResetPreviewResponse(true, "Se este e-mail estiver cadastrado, um código foi enviado.",
                mascararEmail(emailParaMascarar));
    }

    @Transactional
    public ResetValidarResponse validarOtp(String email, String codigo) {
        String emailNorm = email.trim().toLowerCase();

        rateLimiterService.verificarDisponivel(emailNorm);

        Usuario usuario = usuarioRepository.findByEmail(emailNorm).orElse(null);

        if (usuario == null || usuario.getCodigoRecuperacao() == null || usuario.getCodigoRecuperacaoExpiraEm() == null) {
            rateLimiterService.registrarFalha(emailNorm);
            throw new RegraDeNegocioException("Código inválido ou expirado.");
        }

        if (usuario.getCodigoRecuperacaoExpiraEm().isBefore(LocalDateTime.now())) {
            rateLimiterService.registrarFalha(emailNorm);
            throw new RegraDeNegocioException("Código expirado. Solicite um novo.");
        }

        if (!passwordEncoder.matches(codigo, usuario.getCodigoRecuperacao())) {
            rateLimiterService.registrarFalha(emailNorm);
            throw new RegraDeNegocioException("Código inválido.");
        }

        rateLimiterService.registrarSucesso(emailNorm);

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

        // Sem isso, o mesmo token de reset (válido por 15 min) poderia ser reusado
        // várias vezes dentro da janela, inclusive depois que o dono já trocou a senha.
        if (tokenRevogadoRepository.existsByToken(tokenRedefinicao)) {
            throw new RegraDeNegocioException("Token de redefinição inválido ou expirado.");
        }

        UUID userId = jwtTokenProvider.obterSubjectDoToken(tokenRedefinicao);
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (passwordEncoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));

        LocalDateTime expiracaoToken = jwtTokenProvider.obterDataExpiracaoDoToken(tokenRedefinicao);
        tokenRevogadoRepository.save(new TokenRevogado(tokenRedefinicao, LocalDateTime.now(), expiracaoToken));

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