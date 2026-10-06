package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.config.SecurityConfig;
import com.unifapi.academico.domain.enums.StatusConvite;
import com.unifapi.academico.dto.convite.ConviteResponse;
import com.unifapi.academico.dto.convite.CriarConviteRequest;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.ConviteProjetoService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Todos os endpoints exigem autenticação; posse (criador/convidado) é resolvida no service mockado. */
@WebMvcTest(
        controllers = ConviteProjetoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class ConviteProjetoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConviteProjetoService conviteService;

    private ConviteResponse resposta(StatusConvite status) {
        return new ConviteResponse(UUID.randomUUID(), null, null, null, "Backend", null,
                status, LocalDateTime.now(), null);
    }

    @Test
    @WithUsuarioMock
    void convidar_deveRetornar201_quandoDadosValidos() throws Exception {
        CriarConviteRequest request = new CriarConviteRequest(UUID.randomUUID(), UUID.randomUUID(), "Backend", null);
        when(conviteService.convidar(any(CriarConviteRequest.class), any(UUID.class)))
                .thenReturn(resposta(StatusConvite.PENDENTE));

        mockMvc.perform(post("/convites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @WithUsuarioMock
    void convidar_deveRetornar400_quandoUsuarioIdAusente() throws Exception {
        String corpo = "{\"projetoId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/convites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest());

        verify(conviteService, never()).convidar(any(), any());
    }

    @Test
    void convidar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        CriarConviteRequest request = new CriarConviteRequest(UUID.randomUUID(), UUID.randomUUID(), null, null);

        mockMvc.perform(post("/convites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void listarRecebidos_deveRetornar200_comFiltroDeStatus() throws Exception {
        Page<ConviteResponse> pagina = new PageImpl<>(
                List.of(resposta(StatusConvite.PENDENTE)), PageRequest.of(0, 10), 1);
        when(conviteService.listarRecebidos(any(UUID.class), eq(StatusConvite.PENDENTE), any()))
                .thenReturn(pagina);

        mockMvc.perform(get("/convites/recebidos").param("status", "PENDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDENTE"));
    }

    @Test
    @WithUsuarioMock
    void contarPendentes_deveRetornarQuantidade() throws Exception {
        when(conviteService.contarPendentes(any(UUID.class))).thenReturn(2L);

        mockMvc.perform(get("/convites/recebidos/contagem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendentes").value(2));
    }

    @Test
    @WithUsuarioMock
    void aceitar_deveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(conviteService.aceitar(eq(id), any(UUID.class))).thenReturn(resposta(StatusConvite.ACEITO));

        mockMvc.perform(patch("/convites/{id}/aceitar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITO"));
    }

    @Test
    @WithUsuarioMock
    void aceitar_deveRetornar400_quandoRegraViolada() throws Exception {
        UUID id = UUID.randomUUID();
        when(conviteService.aceitar(eq(id), any(UUID.class)))
                .thenThrow(new RegraDeNegocioException("Este convite já foi recusado."));

        mockMvc.perform(patch("/convites/{id}/aceitar", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUsuarioMock
    void recusar_deveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(conviteService.recusar(eq(id), any(UUID.class))).thenReturn(resposta(StatusConvite.RECUSADO));

        mockMvc.perform(patch("/convites/{id}/recusar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADO"));
    }

    @Test
    @WithUsuarioMock
    void cancelar_deveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(conviteService.cancelar(eq(id), any(UUID.class))).thenReturn(resposta(StatusConvite.CANCELADO));

        mockMvc.perform(patch("/convites/{id}/cancelar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    @WithUsuarioMock
    void listarPorProjeto_deveRetornar200() throws Exception {
        UUID projetoId = UUID.randomUUID();
        Page<ConviteResponse> pagina = new PageImpl<>(
                List.of(resposta(StatusConvite.PENDENTE)), PageRequest.of(0, 10), 1);
        when(conviteService.listarPorProjeto(eq(projetoId), any(), any(), any(UUID.class))).thenReturn(pagina);

        mockMvc.perform(get("/convites/projeto/{projetoId}", projetoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].funcao").value("Backend"));
    }
}
