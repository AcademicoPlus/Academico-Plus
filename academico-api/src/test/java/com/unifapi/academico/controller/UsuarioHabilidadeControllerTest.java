package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.domain.enums.NivelHabilidade;
import com.unifapi.academico.dto.habilidade.AdicionarHabilidadeUsuarioRequest;
import com.unifapi.academico.dto.habilidade.AtualizarNivelHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.HabilidadeResumoResponse;
import com.unifapi.academico.dto.habilidade.UsuarioHabilidadeResponse;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.UsuarioHabilidadeService;
import com.unifapi.academico.config.SecurityConfig;
import com.unifapi.academico.support.SecurityTestConfig;
import com.unifapi.academico.support.WithUsuarioMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UsuarioHabilidadeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class UsuarioHabilidadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioHabilidadeService usuarioHabilidadeService;

    private UsuarioHabilidadeResponse respostaPadrao() {
        return new UsuarioHabilidadeResponse(UUID.randomUUID(),
                new HabilidadeResumoResponse(UUID.randomUUID(), "Java", "Backend"), NivelHabilidade.INTERMEDIARIO);
    }

    @Test
    @WithUsuarioMock
    void listar_deveRetornar200_quandoAutenticado() throws Exception {
        when(usuarioHabilidadeService.listar(any(UUID.class))).thenReturn(List.of(respostaPadrao()));

        mockMvc.perform(get("/usuarios/me/habilidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].habilidade.nome").value("Java"));
    }

    @Test
    void listar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/usuarios/me/habilidades"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void adicionar_deveRetornar201_quandoDadosValidos() throws Exception {
        AdicionarHabilidadeUsuarioRequest request = new AdicionarHabilidadeUsuarioRequest(
                UUID.randomUUID(), NivelHabilidade.INICIANTE);
        when(usuarioHabilidadeService.adicionar(any(UUID.class), any(AdicionarHabilidadeUsuarioRequest.class)))
                .thenReturn(respostaPadrao());

        mockMvc.perform(post("/usuarios/me/habilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.habilidade.nome").value("Java"));
    }

    @Test
    @WithUsuarioMock
    void adicionar_deveRetornar400_quandoHabilidadeIdAusente() throws Exception {
        String corpoInvalido = "{}";

        mockMvc.perform(post("/usuarios/me/habilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"));

        verify(usuarioHabilidadeService, never()).adicionar(any(), any());
    }

    @Test
    @WithUsuarioMock
    void adicionar_deveRetornar400_quandoHabilidadeJaEstaNoPerfil() throws Exception {
        AdicionarHabilidadeUsuarioRequest request = new AdicionarHabilidadeUsuarioRequest(
                UUID.randomUUID(), NivelHabilidade.INICIANTE);
        when(usuarioHabilidadeService.adicionar(any(UUID.class), any(AdicionarHabilidadeUsuarioRequest.class)))
                .thenThrow(new RegraDeNegocioException("A habilidade 'Java' já está no seu perfil."));

        mockMvc.perform(post("/usuarios/me/habilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A habilidade 'Java' já está no seu perfil."));
    }

    @Test
    void adicionar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        AdicionarHabilidadeUsuarioRequest request = new AdicionarHabilidadeUsuarioRequest(
                UUID.randomUUID(), NivelHabilidade.INICIANTE);

        mockMvc.perform(post("/usuarios/me/habilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void atualizarNivel_deveRetornar200_quandoVinculoExiste() throws Exception {
        UUID habilidadeId = UUID.randomUUID();
        AtualizarNivelHabilidadeRequest request = new AtualizarNivelHabilidadeRequest(NivelHabilidade.AVANCADO);
        when(usuarioHabilidadeService.atualizarNivel(any(UUID.class), eq(habilidadeId), eq(NivelHabilidade.AVANCADO)))
                .thenReturn(respostaPadrao());

        mockMvc.perform(patch("/usuarios/me/habilidades/{habilidadeId}", habilidadeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void atualizarNivel_deveRetornar400_quandoVinculoNaoExiste() throws Exception {
        UUID habilidadeId = UUID.randomUUID();
        AtualizarNivelHabilidadeRequest request = new AtualizarNivelHabilidadeRequest(NivelHabilidade.AVANCADO);
        when(usuarioHabilidadeService.atualizarNivel(any(UUID.class), eq(habilidadeId), eq(NivelHabilidade.AVANCADO)))
                .thenThrow(new com.unifapi.academico.exception.RecursoNaoEncontradoException(
                        "Esta habilidade não está no seu perfil."));

        mockMvc.perform(patch("/usuarios/me/habilidades/{habilidadeId}", habilidadeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUsuarioMock
    void remover_deveRetornar200_quandoVinculoExiste() throws Exception {
        UUID habilidadeId = UUID.randomUUID();
        doNothing().when(usuarioHabilidadeService).remover(any(UUID.class), eq(habilidadeId));

        mockMvc.perform(delete("/usuarios/me/habilidades/{habilidadeId}", habilidadeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true));
    }

    @Test
    void remover_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(delete("/usuarios/me/habilidades/{habilidadeId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
