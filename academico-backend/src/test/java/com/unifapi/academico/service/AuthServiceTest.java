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
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CursoRepository;
import com.unifapi.academico.repository.NivelAcessoRepository;
import com.unifapi.academico.repository.ProjetoCanceladoAvisoRepository;
import com.unifapi.academico.repository.TokenRevogadoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.security.JwtTokenProvider;
import com.unifapi.academico.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.unifapi.academico.support.EntityFixtures.curso;
import static com.unifapi.academico.support.EntityFixtures.nivelAcesso;
import static com.unifapi.academico.support.EntityFixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários puros (Mockito, sem contexto Spring) para o {@link AuthService} —
 * cobrem login, cadastro, e todo o fluxo de recuperação de senha via OTP.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private NivelAcessoRepository nivelAcessoRepository;
    @Mock private ProjetoCanceladoAvisoRepository projetoAvisoRepository;
    @Mock private TokenRevogadoRepository tokenRevogadoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private CodigoVerificacaoPublisher codigoVerificacaoPublisher;
    @Mock private RateLimiterService rateLimiterService;

    private AuthService authService;

    private NivelAcesso nivelAluno;
    private Curso curso;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                usuarioRepository,
                cursoRepository,
                nivelAcessoRepository,
                projetoAvisoRepository,
                tokenRevogadoRepository,
                passwordEncoder,
                jwtTokenProvider,
                codigoVerificacaoPublisher,
                rateLimiterService
        );

        nivelAluno = nivelAcesso("ALUNO");
        curso = curso("Sistemas de Informacao");
        usuario = usuario("usuario@unifapi.edu.br", "Usuario Teste", nivelAluno, curso);
    }

    // ============================================
    // LOGIN
    // ============================================

    @Test
    void devePropagarExcecaoQuandoRateLimiterBloqueiaLogin() {
        String email = "usuario@unifapi.edu.br";
        LoginRequest request = new LoginRequest(email, "senha123");

        doThrow(new RegraDeNegocioException("Muitas tentativas. Tente novamente mais tarde."))
                .when(rateLimiterService).verificarDisponivel(email);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Muitas tentativas. Tente novamente mais tarde.");

        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    void deveRetornarMensagemGenericaQuandoUsuarioNaoExisteAoLogar() {
        String email = "inexistente@unifapi.edu.br";
        LoginRequest request = new LoginRequest(email, "senha123");

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        LoginResponse response = authService.login(request);

        assertThat(response.sucesso()).isFalse();
        assertThat(response.mensagem()).isEqualTo("Email ou senha incorretos.");
        assertThat(response.token()).isNull();
        assertThat(response.usuario()).isNull();
        assertThat(response.avisosCancelamento()).isEmpty();

        verify(rateLimiterService).registrarFalha(email);
        verify(rateLimiterService, never()).registrarSucesso(any());
    }

    @Test
    void deveRetornarMensagemGenericaQuandoSenhaEstaIncorreta() {
        String email = usuario.getEmail();
        LoginRequest request = new LoginRequest(email, "senhaErrada");

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", usuario.getSenhaHash())).thenReturn(false);

        LoginResponse response = authService.login(request);

        assertThat(response.sucesso()).isFalse();
        assertThat(response.mensagem()).isEqualTo("Email ou senha incorretos.");

        verify(rateLimiterService).registrarFalha(email);
        verify(rateLimiterService, never()).registrarSucesso(any());
        verify(jwtTokenProvider, never()).gerarTokenAcesso(any(), any());
    }

    @Test
    void deveRetornarContaDesativadaQuandoCredenciaisCorretasMasUsuarioInativo() {
        String email = usuario.getEmail();
        usuario.setAtivo(false);
        LoginRequest request = new LoginRequest(email, "senhaCorreta");

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaCorreta", usuario.getSenhaHash())).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertThat(response.sucesso()).isFalse();
        assertThat(response.mensagem()).isEqualTo("Esta conta está desativada.");
        assertThat(response.token()).isNull();

        // A senha bateu, então o rate limiter é resetado mesmo que o login falhe por conta desativada.
        verify(rateLimiterService).registrarSucesso(email);
        verify(rateLimiterService, never()).registrarFalha(any());
        verify(jwtTokenProvider, never()).gerarTokenAcesso(any(), any());
    }

    @Test
    void deveLogarComSucessoEConsumirAvisoDeProjetoCanceladoPendente() {
        String email = usuario.getEmail();
        LoginRequest request = new LoginRequest(email, "senhaCorreta");

        ProjetoCanceladoAviso aviso = ProjetoCanceladoAviso.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .projetoNome("Projeto X")
                .projetoData(java.time.LocalDate.now().plusDays(1))
                .tipo("cancelamento")
                .criadoEm(LocalDateTime.now())
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaCorreta", usuario.getSenhaHash())).thenReturn(true);
        when(jwtTokenProvider.gerarTokenAcesso(usuario.getId(), nivelAluno.getNome())).thenReturn("token-jwt");
        when(projetoAvisoRepository.findByUsuario_IdAndVisualizadoEmIsNullOrderByCriadoEmAsc(usuario.getId()))
                .thenReturn(List.of(aviso));

        LoginResponse response = authService.login(request);

        assertThat(response.sucesso()).isTrue();
        assertThat(response.mensagem()).isEqualTo("Login realizado com sucesso.");
        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.usuario()).isNotNull();
        assertThat(response.usuario().id()).isEqualTo(usuario.getId());
        assertThat(response.avisosCancelamento()).hasSize(1);
        assertThat(response.avisosCancelamento().get(0).nome()).isEqualTo("Projeto X");

        // O aviso é marcado como visualizado (consumido) durante o login — não há um save() explícito
        // no repositório, pois a transação do @Transactional persiste a mudança via dirty checking.
        assertThat(aviso.getVisualizadoEm()).isNotNull();

        verify(rateLimiterService).registrarSucesso(email);
    }

    @Test
    void deveRetornarListaDeAvisosVaziaQuandoNaoHaAvisosPendentes() {
        String email = usuario.getEmail();
        LoginRequest request = new LoginRequest(email, "senhaCorreta");

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaCorreta", usuario.getSenhaHash())).thenReturn(true);
        when(jwtTokenProvider.gerarTokenAcesso(usuario.getId(), nivelAluno.getNome())).thenReturn("token-jwt");
        // Simula uma segunda chamada de login: o aviso já foi consumido/marcado como visualizado antes,
        // então a query não retorna mais nada pendente.
        when(projetoAvisoRepository.findByUsuario_IdAndVisualizadoEmIsNullOrderByCriadoEmAsc(usuario.getId()))
                .thenReturn(List.of());

        LoginResponse response = authService.login(request);

        assertThat(response.sucesso()).isTrue();
        assertThat(response.avisosCancelamento()).isEmpty();
    }

    // ============================================
    // CADASTRO
    // ============================================

    @Test
    void deveLancarExcecaoQuandoEmailJaCadastrado() {
        CadastroRequest request = new CadastroRequest(
                "novo@unifapi.edu.br", "Novo Usuario", 1, curso.getId(), "senha12345", true);

        when(usuarioRepository.existsByEmail("novo@unifapi.edu.br")).thenReturn(true);

        assertThatThrownBy(() -> authService.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já está cadastrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoCursoNaoEncontrado() {
        UUID idCursoInexistente = UUID.randomUUID();
        CadastroRequest request = new CadastroRequest(
                "novo@unifapi.edu.br", "Novo Usuario", 1, idCursoInexistente, "senha12345", true);

        when(usuarioRepository.existsByEmail("novo@unifapi.edu.br")).thenReturn(false);
        when(cursoRepository.findById(idCursoInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.cadastrar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Curso não encontrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoNivelAcessoAlunoNaoEncontrado() {
        CadastroRequest request = new CadastroRequest(
                "novo@unifapi.edu.br", "Novo Usuario", 1, curso.getId(), "senha12345", true);

        when(usuarioRepository.existsByEmail("novo@unifapi.edu.br")).thenReturn(false);
        when(cursoRepository.findById(curso.getId())).thenReturn(Optional.of(curso));
        when(nivelAcessoRepository.findByNome("ALUNO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.cadastrar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Nível de acesso padrão não encontrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveCadastrarUsuarioComSucesso() {
        CadastroRequest request = new CadastroRequest(
                "Novo@Unifapi.edu.br", "Novo Usuario", 2, curso.getId(), "senha12345", true);

        when(usuarioRepository.existsByEmail("novo@unifapi.edu.br")).thenReturn(false);
        when(cursoRepository.findById(curso.getId())).thenReturn(Optional.of(curso));
        when(nivelAcessoRepository.findByNome("ALUNO")).thenReturn(Optional.of(nivelAluno));
        when(passwordEncoder.encode("senha12345")).thenReturn("hash-bcrypt-simulado");

        CadastroResponse response = authService.cadastrar(request);

        assertThat(response.sucesso()).isTrue();
        assertThat(response.mensagem()).isEqualTo("Cadastro realizado com sucesso.");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();

        assertThat(salvo.getEmail()).isEqualTo("novo@unifapi.edu.br");
        assertThat(salvo.getAtivo()).isTrue();
        assertThat(salvo.getSenhaHash()).isEqualTo("hash-bcrypt-simulado");
        assertThat(salvo.getNivelAcesso()).isEqualTo(nivelAluno);
        assertThat(salvo.getCurso()).isEqualTo(curso);

        verify(passwordEncoder).encode("senha12345");
    }

    // ============================================
    // SOLICITAR RESET DE SENHA (OTP)
    // ============================================

    @Test
    void deveRetornarMensagemGenericaQuandoUsuarioNaoExisteAoSolicitarReset() {
        String email = "fantasma@unifapi.edu.br";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        MensagemResponse response = authService.solicitarResetSenha(email);

        assertThat(response.sucesso()).isTrue();
        assertThat(response.mensagem())
                .isEqualTo("Se este e-mail estiver cadastrado, um código de verificação foi enviado.");

        verifyNoInteractions(codigoVerificacaoPublisher);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveRetornarMesmaMensagemGenericaQuandoUsuarioExisteAoSolicitarReset() {
        String email = usuario.getEmail();
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-otp");

        MensagemResponse response = authService.solicitarResetSenha(email);

        // Mesma mensagem do caso "usuário não existe" — evita oráculo de enumeração de e-mails.
        assertThat(response.sucesso()).isTrue();
        assertThat(response.mensagem())
                .isEqualTo("Se este e-mail estiver cadastrado, um código de verificação foi enviado.");

        assertThat(usuario.getCodigoRecuperacao()).isEqualTo("hash-otp");
        assertThat(usuario.getCodigoRecuperacaoExpiraEm())
                .isAfter(LocalDateTime.now().plusMinutes(14))
                .isBefore(LocalDateTime.now().plusMinutes(16));

        verify(usuarioRepository).save(usuario);
        // Fire-and-forget: publicar na fila é o único envolvimento do AuthService com o
        // envio — falhas reais de entrega não voltam mais pro chamador HTTP (ver academico-email-service).
        verify(codigoVerificacaoPublisher).publicar(eq(usuario.getEmail()), anyString(), eq(15));
    }

    @Test
    void deveLancarExcecaoQuandoEmailNaoInformadoAoSolicitarReset() {
        assertThatThrownBy(() -> authService.solicitarResetSenha(" "))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Informe o e-mail.");

        verifyNoInteractions(usuarioRepository);
    }

    // ============================================
    // PREVIEW DO ALVO DO RESET (mascaramento de e-mail)
    // ============================================

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExisteAoPreverReset() {
        String email = "fantasma@unifapi.edu.br";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Ao contrário de solicitarResetSenha, este método É um oráculo deliberado de enumeração.
        assertThatThrownBy(() -> authService.previewResetTarget(email))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Nenhum usuário encontrado");
    }

    @Test
    void deveMascararEmailComParteLocalCurta() {
        Usuario alvo = usuario("ab@x.com", "Ana Beatriz", nivelAluno, curso);
        when(usuarioRepository.findByEmail("ab@x.com")).thenReturn(Optional.of(alvo));

        ResetPreviewResponse response = authService.previewResetTarget("ab@x.com");

        assertThat(response.sucesso()).isTrue();
        // local.length() <= 2 -> primeiro char + um único '*'
        assertThat(response.emailPreview()).isEqualTo("a*@x.com");
    }

    @Test
    void deveMascararEmailComParteLocalLonga() {
        Usuario alvo = usuario("carlos@x.com", "Carlos Silva", nivelAluno, curso);
        when(usuarioRepository.findByEmail("carlos@x.com")).thenReturn(Optional.of(alvo));

        ResetPreviewResponse response = authService.previewResetTarget("carlos@x.com");

        assertThat(response.sucesso()).isTrue();
        // local.length() > 2 -> primeiros 2 chars + (length - 2) asteriscos ("carlos" tem 6 chars -> 4 '*')
        assertThat(response.emailPreview()).isEqualTo("ca****@x.com");
    }

    // ============================================
    // VALIDAR OTP
    // ============================================

    @Test
    void devePropagarExcecaoQuandoRateLimiterBloqueiaValidacaoDeOtp() {
        String email = usuario.getEmail();
        doThrow(new RegraDeNegocioException("Muitas tentativas. Tente novamente mais tarde."))
                .when(rateLimiterService).verificarDisponivel(email);

        assertThatThrownBy(() -> authService.validarOtp(email, "123456"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Muitas tentativas. Tente novamente mais tarde.");

        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    void deveLancarExcecaoGenericaQuandoUsuarioNaoExisteAoValidarOtp() {
        String email = "fantasma@unifapi.edu.br";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.validarOtp(email, "123456"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Código inválido ou expirado.");

        verify(rateLimiterService).registrarFalha(email);
    }

    @Test
    void deveLancarExcecaoGenericaQuandoCodigoRecuperacaoNuloAoValidarOtp() {
        String email = usuario.getEmail();
        usuario.setCodigoRecuperacao(null);
        usuario.setCodigoRecuperacaoExpiraEm(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.validarOtp(email, "123456"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Código inválido ou expirado.");

        verify(rateLimiterService).registrarFalha(email);
    }

    @Test
    void deveLancarExcecaoGenericaQuandoExpiracaoNulaAoValidarOtp() {
        String email = usuario.getEmail();
        usuario.setCodigoRecuperacao("hash-otp");
        usuario.setCodigoRecuperacaoExpiraEm(null);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.validarOtp(email, "123456"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Código inválido ou expirado.");

        verify(rateLimiterService).registrarFalha(email);
    }

    @Test
    void deveLancarExcecaoDeCodigoExpiradoQuandoOtpJaExpirou() {
        String email = usuario.getEmail();
        usuario.setCodigoRecuperacao("hash-otp");
        usuario.setCodigoRecuperacaoExpiraEm(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.validarOtp(email, "123456"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Código expirado. Solicite um novo.");

        verify(rateLimiterService).registrarFalha(email);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void deveLancarExcecaoDeCodigoInvalidoQuandoHashNaoCoincide() {
        String email = usuario.getEmail();
        usuario.setCodigoRecuperacao("hash-otp");
        usuario.setCodigoRecuperacaoExpiraEm(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("000000", "hash-otp")).thenReturn(false);

        assertThatThrownBy(() -> authService.validarOtp(email, "000000"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Código inválido.");

        verify(rateLimiterService).registrarFalha(email);
    }

    @Test
    void deveValidarOtpComSucessoELimparCodigoDeUsoUnico() {
        String email = usuario.getEmail();
        usuario.setCodigoRecuperacao("hash-otp");
        usuario.setCodigoRecuperacaoExpiraEm(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hash-otp")).thenReturn(true);
        when(jwtTokenProvider.gerarTokenResetSenha(usuario.getId())).thenReturn("token-reset");

        ResetValidarResponse response = authService.validarOtp(email, "123456");

        assertThat(response.sucesso()).isTrue();
        assertThat(response.tokenRedefinicao()).isEqualTo("token-reset");

        assertThat(usuario.getCodigoRecuperacao()).isNull();
        assertThat(usuario.getCodigoRecuperacaoExpiraEm()).isNull();

        verify(rateLimiterService).registrarSucesso(email);
        verify(rateLimiterService, never()).registrarFalha(any());
    }

    // ============================================
    // REDEFINIR SENHA
    // ============================================

    @Test
    void deveLancarExcecaoQuandoTokenDeResetInvalido() {
        String token = "token-invalido";
        when(jwtTokenProvider.validarTokenReset(token)).thenReturn(false);

        assertThatThrownBy(() -> authService.redefinirSenha(token, "novaSenha123"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Token de redefinição inválido ou expirado.");

        verify(tokenRevogadoRepository, never()).existsByToken(any());
    }

    @Test
    void deveLancarExcecaoQuandoTokenDeResetJaFoiUsadoAntes() {
        String token = "token-ja-usado";
        when(jwtTokenProvider.validarTokenReset(token)).thenReturn(true);
        when(tokenRevogadoRepository.existsByToken(token)).thenReturn(true);

        assertThatThrownBy(() -> authService.redefinirSenha(token, "novaSenha123"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Token de redefinição inválido ou expirado.");

        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioDoTokenNaoEncontrado() {
        String token = "token-valido";
        UUID userId = UUID.randomUUID();
        when(jwtTokenProvider.validarTokenReset(token)).thenReturn(true);
        when(tokenRevogadoRepository.existsByToken(token)).thenReturn(false);
        when(jwtTokenProvider.obterSubjectDoToken(token)).thenReturn(userId);
        when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.redefinirSenha(token, "novaSenha123"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Usuário não encontrado.");
    }

    @Test
    void deveLancarExcecaoQuandoNovaSenhaIgualASenhaAtual() {
        String token = "token-valido";
        when(jwtTokenProvider.validarTokenReset(token)).thenReturn(true);
        when(tokenRevogadoRepository.existsByToken(token)).thenReturn(false);
        when(jwtTokenProvider.obterSubjectDoToken(token)).thenReturn(usuario.getId());
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaRepetida", usuario.getSenhaHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.redefinirSenha(token, "senhaRepetida"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("A nova senha deve ser diferente da senha atual.");

        verify(tokenRevogadoRepository, never()).save(any());
    }

    @Test
    void deveRedefinirSenhaComSucessoERevogarToken() {
        String token = "token-valido";
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(15);
        when(jwtTokenProvider.validarTokenReset(token)).thenReturn(true);
        when(tokenRevogadoRepository.existsByToken(token)).thenReturn(false);
        when(jwtTokenProvider.obterSubjectDoToken(token)).thenReturn(usuario.getId());
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("novaSenhaSegura", usuario.getSenhaHash())).thenReturn(false);
        when(passwordEncoder.encode("novaSenhaSegura")).thenReturn("novo-hash-bcrypt");
        when(jwtTokenProvider.obterDataExpiracaoDoToken(token)).thenReturn(expiracao);

        MensagemResponse response = authService.redefinirSenha(token, "novaSenhaSegura");

        assertThat(response.sucesso()).isTrue();
        assertThat(response.mensagem()).isEqualTo("Senha redefinida com sucesso.");
        assertThat(usuario.getSenhaHash()).isEqualTo("novo-hash-bcrypt");

        ArgumentCaptor<TokenRevogado> captor = ArgumentCaptor.forClass(TokenRevogado.class);
        verify(tokenRevogadoRepository, times(1)).save(captor.capture());
        TokenRevogado revogado = captor.getValue();
        assertThat(revogado.getToken()).isEqualTo(token);
        assertThat(revogado.getDataExpiracao()).isEqualTo(expiracao);
    }
}
