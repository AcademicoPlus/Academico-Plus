package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.dto.candidatura.AceitarCandidaturaRequest;
import com.unifapi.academico.dto.candidatura.CandidaturaResponse;
import com.unifapi.academico.dto.candidatura.CriarCandidaturaRequest;
import com.unifapi.academico.dto.candidatura.RejeitarCandidaturaRequest;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.CandidaturaService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Todos os endpoints exigem autenticação; posse (candidato/criador) é resolvida no service mockado. */
@WebMvcTest(
        controllers = CandidaturaController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class CandidaturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CandidaturaService candidaturaService;

    private CandidaturaResponse respostaPadrao() {
        return new CandidaturaResponse(UUID.randomUUID(), null, null, StatusCandidatura.PENDENTE,
                "Tenho interesse", null, LocalDateTime.now(), null);
    }

    @Test
    @WithUsuarioMock
    void candidatar_deveRetornar201_quandoDadosValidos() throws Exception {
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(UUID.randomUUID(), "Tenho interesse");
        when(candidaturaService.candidatar(any(CriarCandidaturaRequest.class), any(UUID.class)))
                .thenReturn(respostaPadrao());

        mockMvc.perform(post("/candidaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @WithUsuarioMock
    void candidatar_deveRetornar400_quandoProjetoIdAusente() throws Exception {
        String corpoInvalido = "{}";

        mockMvc.perform(post("/candidaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"));

        verify(candidaturaService, never()).candidatar(any(), any());
    }

    @Test
    void candidatar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        CriarCandidaturaRequest request = new CriarCandidaturaRequest(UUID.randomUUID(), "Tenho interesse");

        mockMvc.perform(post("/candidaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void listarMinhas_deveRetornar200_comFiltroDeStatus() throws Exception {
        Page<CandidaturaResponse> pagina = new PageImpl<>(List.of(respostaPadrao()), PageRequest.of(0, 10), 1);
        when(candidaturaService.listarMinhas(any(UUID.class), eq(StatusCandidatura.PENDENTE), any()))
                .thenReturn(pagina);

        mockMvc.perform(get("/candidaturas/minhas").param("status", "PENDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDENTE"));
    }

    @Test
    void listarMinhas_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/candidaturas/minhas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void cancelar_deveRetornar200_quandoCandidatoDono() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(candidaturaService).cancelar(eq(id), any(UUID.class));

        mockMvc.perform(delete("/candidaturas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true));
    }

    @Test
    @WithUsuarioMock
    void cancelar_deveRetornar400_quandoJaRespondida() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RegraDeNegocioException("Esta candidatura já foi aceito e não pode ser cancelada."))
                .when(candidaturaService).cancelar(eq(id), any(UUID.class));

        mockMvc.perform(delete("/candidaturas/{id}", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUsuarioMock
    void listarPorProjeto_deveRetornar200_quandoCriador() throws Exception {
        UUID projetoId = UUID.randomUUID();
        Page<CandidaturaResponse> pagina = new PageImpl<>(List.of(respostaPadrao()), PageRequest.of(0, 10), 1);
        when(candidaturaService.listarPorProjeto(eq(projetoId), any(), any(), any(UUID.class)))
                .thenReturn(pagina);

        mockMvc.perform(get("/candidaturas/projeto/{projetoId}", projetoId))
                .andExpect(status().isOk());
    }

    @Test
    void listarPorProjeto_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/candidaturas/projeto/{projetoId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void aceitar_deveRetornar200_semCorpoDeRequisicao() throws Exception {
        UUID id = UUID.randomUUID();
        when(candidaturaService.aceitar(eq(id), isNull(), any(UUID.class))).thenReturn(respostaPadrao());

        mockMvc.perform(patch("/candidaturas/{id}/aceitar", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void aceitar_deveRetornar200_comCorpoDeRequisicao() throws Exception {
        UUID id = UUID.randomUUID();
        AceitarCandidaturaRequest request = new AceitarCandidaturaRequest("Desenvolvedor Backend");
        when(candidaturaService.aceitar(eq(id), any(AceitarCandidaturaRequest.class), any(UUID.class)))
                .thenReturn(respostaPadrao());

        mockMvc.perform(patch("/candidaturas/{id}/aceitar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void aceitar_deveRetornar400_quandoJaRespondida() throws Exception {
        UUID id = UUID.randomUUID();
        when(candidaturaService.aceitar(eq(id), isNull(), any(UUID.class)))
                .thenThrow(new RegraDeNegocioException("Esta candidatura já foi respondida."));

        mockMvc.perform(patch("/candidaturas/{id}/aceitar", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUsuarioMock
    void rejeitar_deveRetornar200_quandoMotivoValido() throws Exception {
        UUID id = UUID.randomUUID();
        RejeitarCandidaturaRequest request = new RejeitarCandidaturaRequest("Perfil não compatível com a vaga.");
        when(candidaturaService.rejeitar(eq(id), any(RejeitarCandidaturaRequest.class), any(UUID.class)))
                .thenReturn(respostaPadrao());

        mockMvc.perform(patch("/candidaturas/{id}/rejeitar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void rejeitar_deveRetornar400_quandoMotivoAusente() throws Exception {
        UUID id = UUID.randomUUID();
        String corpoInvalido = "{}";

        mockMvc.perform(patch("/candidaturas/{id}/rejeitar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"));

        verify(candidaturaService, never()).rejeitar(any(), any(), any());
    }

    @Test
    @WithUsuarioMock
    void buscarPorId_deveRetornar200_quandoCandidatoOuCriador() throws Exception {
        UUID id = UUID.randomUUID();
        when(candidaturaService.buscarPorId(eq(id), any(UUID.class))).thenReturn(respostaPadrao());

        mockMvc.perform(get("/candidaturas/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/candidaturas/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
