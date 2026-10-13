package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.dto.auth.CadastroRequest;
import com.unifapi.academico.dto.auth.CadastroResponse;
import com.unifapi.academico.dto.auth.LoginRequest;
import com.unifapi.academico.dto.auth.LoginResponse;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.auth.ResetPreviewRequest;
import com.unifapi.academico.dto.auth.ResetPreviewResponse;
import com.unifapi.academico.dto.auth.ResetRedefinirRequest;
import com.unifapi.academico.dto.auth.ResetSolicitarRequest;
import com.unifapi.academico.dto.auth.ResetValidarRequest;
import com.unifapi.academico.dto.auth.ResetValidarResponse;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.AuthService;
import com.unifapi.academico.config.SecurityConfig;
import com.unifapi.academico.support.SecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Todos os endpoints de {@link AuthController} são {@code permitAll} em SecurityConfig
 * (/auth/**), então nenhum teste aqui usa {@code @WithUsuarioMock}.
 */
@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
    )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;

    // ============================================
    // POST /auth/login
    // ============================================

    @Test
    void deveRetornar200ComTokenQuandoLoginValido() throws Exception {
        LoginRequest request = new LoginRequest("usuario@unifapi.edu.br", "senha12345");
        UsuarioResumoResponse resumo = new UsuarioResumoResponse(
                UUID.randomUUID(), "Usuario Teste",
                "Engenharia de Software", null, "ALUNO", 3, 4.5, 2);
        LoginResponse response = new LoginResponse(true, "Login realizado com sucesso.",
                "jwt-token-fake", resumo, List.of(), List.of());

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.token").value("jwt-token-fake"))
                .andExpect(jsonPath("$.usuario.nome").value("Usuario Teste"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void deveRetornar400ComErroDeValidacaoQuandoEmailAusenteNoLogin() throws Exception {
        String corpoInvalido = """
                {
                    "senha": "senha12345"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"))
                .andExpect(jsonPath("$.erros").isArray())
                .andExpect(jsonPath("$.erros[0].campo").value("email"));

        verify(authService, never()).login(any(LoginRequest.class));
    }

    // ============================================
    // POST /auth/cadastro
    // ============================================

    @Test
    void deveRetornar201QuandoCadastroValido() throws Exception {
        CadastroRequest request = new CadastroRequest(
                "novo@unifapi.edu.br", "Novo Usuario", 1, UUID.randomUUID(), "senha12345", true);
        CadastroResponse response = new CadastroResponse(true, "Cadastro realizado com sucesso.");

        when(authService.cadastrar(any(CadastroRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/cadastro")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Cadastro realizado com sucesso."));

        verify(authService).cadastrar(any(CadastroRequest.class));
    }

    @Test
    void deveRetornar400QuandoCadastroNaoAceitaTermos() throws Exception {
        CadastroRequest request = new CadastroRequest(
                "novo@unifapi.edu.br", "Novo Usuario", 1, UUID.randomUUID(), "senha12345", false);

        mockMvc.perform(post("/auth/cadastro")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"))
                .andExpect(jsonPath("$.erros").isArray());

        verify(authService, never()).cadastrar(any(CadastroRequest.class));
    }

    // ============================================
    // POST /auth/reset-senha/solicitar
    // ============================================

    @Test
    void deveRetornar200QuandoSolicitarResetSenha() throws Exception {
        ResetSolicitarRequest request = new ResetSolicitarRequest("usuario@unifapi.edu.br");
        MensagemResponse response = new MensagemResponse(true,
                "Se este e-mail estiver cadastrado, um código de verificação foi enviado.");

        when(authService.solicitarResetSenha(anyString())).thenReturn(response);

        mockMvc.perform(post("/auth/reset-senha/solicitar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true));

        verify(authService).solicitarResetSenha("usuario@unifapi.edu.br");
    }

    @Test
    void deveRetornar400QuandoEmailAusenteAoSolicitarReset() throws Exception {
        String corpoInvalido = "{}";

        mockMvc.perform(post("/auth/reset-senha/solicitar")
                        .contentType("application/json")
                        .content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"))
                .andExpect(jsonPath("$.erros[0].campo").value("email"));

        verify(authService, never()).solicitarResetSenha(anyString());
    }

    // ============================================
    // POST /auth/reset-senha/preview
    // ============================================

    @Test
    void deveRetornar200ComPreviewQuandoResetPreviewValido() throws Exception {
        ResetPreviewRequest request = new ResetPreviewRequest("usuario@unifapi.edu.br");
        ResetPreviewResponse response = new ResetPreviewResponse(true,
                "Usuário encontrado para recuperação.", "us***@unifapi.edu.br");

        when(authService.previewResetTarget(anyString())).thenReturn(response);

        mockMvc.perform(post("/auth/reset-senha/preview")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.emailPreview").value("us***@unifapi.edu.br"));

        verify(authService).previewResetTarget("usuario@unifapi.edu.br");
    }

    // ============================================
    // POST /auth/reset-senha/validar
    // ============================================

    @Test
    void deveRetornar200ComTokenDeRedefinicaoQuandoResetValidarValido() throws Exception {
        ResetValidarRequest request = new ResetValidarRequest("usuario@unifapi.edu.br", "123456");
        ResetValidarResponse response = new ResetValidarResponse(true, "token-de-redefinicao-fake");

        when(authService.validarOtp(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/auth/reset-senha/validar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.tokenRedefinicao").value("token-de-redefinicao-fake"));

        verify(authService).validarOtp("usuario@unifapi.edu.br", "123456");
    }

    @Test
    void deveRetornar400QuandoCodigoAusenteAoValidarReset() throws Exception {
        String corpoInvalido = """
                {
                    "email": "usuario@unifapi.edu.br"
                }
                """;

        mockMvc.perform(post("/auth/reset-senha/validar")
                        .contentType("application/json")
                        .content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"))
                .andExpect(jsonPath("$.erros[0].campo").value("codigo"));

        verify(authService, never()).validarOtp(anyString(), anyString());
    }

    // ============================================
    // POST /auth/reset-senha/redefinir
    // ============================================

    @Test
    void deveRetornar200QuandoResetRedefinirValido() throws Exception {
        ResetRedefinirRequest request = new ResetRedefinirRequest("token-de-redefinicao-fake", "novaSenha123");
        MensagemResponse response = new MensagemResponse(true, "Senha redefinida com sucesso.");

        when(authService.redefinirSenha(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/auth/reset-senha/redefinir")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Senha redefinida com sucesso."));

        verify(authService).redefinirSenha("token-de-redefinicao-fake", "novaSenha123");
    }

    @Test
    void deveRetornar400QuandoNovaSenhaCurtaAoRedefinir() throws Exception {
        ResetRedefinirRequest request = new ResetRedefinirRequest("token-de-redefinicao-fake", "curta");

        mockMvc.perform(post("/auth/reset-senha/redefinir")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"))
                .andExpect(jsonPath("$.erros[0].campo").value("novaSenha"));

        verify(authService, never()).redefinirSenha(anyString(), anyString());
    }
}
