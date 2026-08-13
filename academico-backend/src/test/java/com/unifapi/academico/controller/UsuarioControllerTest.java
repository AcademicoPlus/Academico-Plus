package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.avaliacao.AvaliacaoResponse;
import com.unifapi.academico.dto.usuario.ExcluirContaRequest;
import com.unifapi.academico.dto.usuario.UsuarioPerfilResponse;
import com.unifapi.academico.dto.usuario.UsuarioUpdatePerfilRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaResponse;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.AvaliacaoService;
import com.unifapi.academico.service.UsuarioService;
import com.unifapi.academico.config.SecurityConfig;
import com.unifapi.academico.support.SecurityTestConfig;
import com.unifapi.academico.support.WithUsuarioMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UsuarioController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private AvaliacaoService avaliacaoService;

    private UsuarioPerfilResponse perfilPadrao(UUID id) {
        return new UsuarioPerfilResponse(id, "usuario@unifapi.edu.br", "Usuario Teste", null, null,
                3, "bio", null, null, null, List.of(), null, 0, LocalDateTime.now(), true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    // ============================================
    // /me
    // ============================================

    @Test
    @WithUsuarioMock
    void obterMeuPerfil_deveRetornar200_quandoAutenticado() throws Exception {
        when(usuarioService.obterPorId(any(UUID.class))).thenReturn(perfilPadrao(UUID.randomUUID()));

        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("usuario@unifapi.edu.br"));
    }

    @Test
    void obterMeuPerfil_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void atualizarMeuPerfil_deveRetornar200_quandoDadosValidos() throws Exception {
        UsuarioUpdatePerfilRequest request = new UsuarioUpdatePerfilRequest(
                "Novo Nome", null, 4, "nova bio", null, null, null);
        when(usuarioService.atualizarPerfil(any(UUID.class), any(UsuarioUpdatePerfilRequest.class)))
                .thenReturn(perfilPadrao(UUID.randomUUID()));

        mockMvc.perform(put("/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void atualizarMeuPerfil_deveRetornar400_quandoNomeEmBranco() throws Exception {
        UsuarioUpdatePerfilRequest request = new UsuarioUpdatePerfilRequest(
                "", null, 4, "nova bio", null, null, null);

        mockMvc.perform(put("/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"));

        verify(usuarioService, never()).atualizarPerfil(any(), any());
    }

    @Test
    @WithUsuarioMock
    void alterarSenha_deveRetornar200_quandoDadosValidos() throws Exception {
        UsuarioUpdateSenhaRequest request = new UsuarioUpdateSenhaRequest("senhaAtual123", "senhaNova123");
        when(usuarioService.atualizarSenha(any(UUID.class), any(UsuarioUpdateSenhaRequest.class)))
                .thenReturn(UsuarioUpdateSenhaResponse.sucesso());

        mockMvc.perform(patch("/usuarios/me/senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Senha alterada com sucesso."));
    }

    @Test
    @WithUsuarioMock
    void alterarSenha_deveRetornar400_quandoRegraDeNegocioViolada() throws Exception {
        UsuarioUpdateSenhaRequest request = new UsuarioUpdateSenhaRequest("senhaErrada", "senhaNova123");
        when(usuarioService.atualizarSenha(any(UUID.class), any(UsuarioUpdateSenhaRequest.class)))
                .thenThrow(new RegraDeNegocioException("Senha atual incorreta."));

        mockMvc.perform(patch("/usuarios/me/senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Senha atual incorreta."));
    }

    @Test
    @WithUsuarioMock
    void uploadFoto_deveRetornar200_quandoArquivoValido() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("foto", "foto.png", "image/png", "conteudo".getBytes());
        when(usuarioService.atualizarFoto(any(UUID.class), any())).thenReturn(perfilPadrao(UUID.randomUUID()));

        mockMvc.perform(multipart("/usuarios/me/foto").file(arquivo))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void excluirMinhaConta_deveRetornar200_quandoSenhaCorreta() throws Exception {
        ExcluirContaRequest request = new ExcluirContaRequest("senhaCorreta123");
        doNothing().when(usuarioService).excluirMinhaConta(any(UUID.class), any(ExcluirContaRequest.class));

        mockMvc.perform(delete("/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true));
    }

    @Test
    @WithUsuarioMock
    void excluirMinhaConta_deveRetornar400_quandoSenhaIncorreta() throws Exception {
        ExcluirContaRequest request = new ExcluirContaRequest("senhaErrada");
        doThrow(new RegraDeNegocioException("Senha incorreta."))
                .when(usuarioService).excluirMinhaConta(any(UUID.class), any(ExcluirContaRequest.class));

        mockMvc.perform(delete("/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void excluirMinhaConta_deveRetornar403_quandoNaoAutenticado() throws Exception {
        ExcluirContaRequest request = new ExcluirContaRequest("senhaCorreta123");

        mockMvc.perform(delete("/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============================================
    // /logout
    // ============================================

    @Test
    @WithUsuarioMock
    void logout_deveRetornar200_quandoAutenticado() throws Exception {
        when(usuarioService.logout(anyString()))
                .thenReturn(new MensagemResponse(true, "Logout realizado com sucesso."));

        mockMvc.perform(post("/usuarios/logout").header("Authorization", "Bearer token-fake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true));
    }

    // ============================================
    // ADMIN
    // ============================================

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void alterarNivelAcesso_devePermitir_quandoAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        UUID idNivel = UUID.randomUUID();
        when(usuarioService.alterarNivelAcesso(eq(id), eq(idNivel))).thenReturn(perfilPadrao(id));

        mockMvc.perform(patch("/usuarios/{id}/nivel-acesso", id).param("idNivel", idNivel.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void alterarNivelAcesso_deveRetornar403_quandoAluno() throws Exception {
        UUID id = UUID.randomUUID();
        UUID idNivel = UUID.randomUUID();

        mockMvc.perform(patch("/usuarios/{id}/nivel-acesso", id).param("idNivel", idNivel.toString()))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).alterarNivelAcesso(any(), any());
    }

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void listarUsuarios_devePermitir_quandoAdmin() throws Exception {
        UsuarioResumoResponse resumo = new UsuarioResumoResponse(
                UUID.randomUUID(), "Usuario", "usuario@unifapi.edu.br", "Curso", null, "ALUNO", 1, null, 0);
        when(usuarioService.listarTodos()).thenReturn(List.of(resumo));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("usuario@unifapi.edu.br"));
    }

    @Test
    @WithUsuarioMock
    void listarUsuarios_deveRetornar403_quandoAluno() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).listarTodos();
    }

    @Test
    void listarUsuarios_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isForbidden());
    }

    // ============================================
    // /explorar, /{id}, /{id}/avaliacoes
    // ============================================

    @Test
    @WithUsuarioMock
    void explorarPerfis_deveRetornar200_quandoAutenticado() throws Exception {
        UsuarioResumoResponse resumo = new UsuarioResumoResponse(
                UUID.randomUUID(), "Usuario", "usuario@unifapi.edu.br", "Curso", null, "ALUNO", 1, null, 0);
        Page<UsuarioResumoResponse> pagina = new PageImpl<>(List.of(resumo), PageRequest.of(0, 12), 1);
        when(usuarioService.buscarPerfisPublicos(any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/usuarios/explorar").param("busca", "usuario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Usuario"));
    }

    @Test
    void explorarPerfis_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/usuarios/explorar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void obterPorId_deveRetornar200_quandoAutenticado() throws Exception {
        UUID id = UUID.randomUUID();
        when(usuarioService.obterPorId(eq(id))).thenReturn(perfilPadrao(id));

        mockMvc.perform(get("/usuarios/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void listarAvaliacoesRecebidas_deveRetornar200_quandoAutenticado() throws Exception {
        UUID id = UUID.randomUUID();
        Page<AvaliacaoResponse> pagina = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(avaliacaoService.listarPorAvaliado(eq(id), any())).thenReturn(pagina);

        mockMvc.perform(get("/usuarios/{id}/avaliacoes", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listarAvaliacoesRecebidas_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/usuarios/{id}/avaliacoes", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
