package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.dto.habilidade.HabilidadeResumoResponse;
import com.unifapi.academico.dto.habilidade.ProjetoHabilidadeResponse;
import com.unifapi.academico.dto.habilidade.VincularHabilidadeRequest;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.ProjetoHabilidadeService;
import com.unifapi.academico.config.SecurityConfig;
import com.unifapi.academico.support.SecurityTestConfig;
import com.unifapi.academico.support.WithUsuarioMock;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ProjetoHabilidadeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class ProjetoHabilidadeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProjetoHabilidadeService projetoHabilidadeService;

    private static ProjetoHabilidadeResponse vinculoResponse(UUID id, UUID habilidadeId, boolean obrigatoria) {
        return new ProjetoHabilidadeResponse(
                id,
                new HabilidadeResumoResponse(habilidadeId, "Java", "Backend"),
                obrigatoria
        );
    }

    // ============================================
    // GET /projetos/{projetoId}/habilidades
    // ============================================

    @Test
    @WithUsuarioMock
    void listar_deveRetornarHabilidadesDoProjeto() throws Exception {
        UUID projetoId = UUID.randomUUID();
        ProjetoHabilidadeResponse resposta = vinculoResponse(UUID.randomUUID(), UUID.randomUUID(), true);

        when(projetoHabilidadeService.listarPorProjeto(projetoId)).thenReturn(List.of(resposta));

        mockMvc.perform(get("/projetos/{projetoId}/habilidades", projetoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].habilidade.nome").value("Java"))
                .andExpect(jsonPath("$[0].obrigatoria").value(true));

        verify(projetoHabilidadeService).listarPorProjeto(projetoId);
    }

    @Test
    void listar_semAutenticacao_deveRetornar403() throws Exception {
        UUID projetoId = UUID.randomUUID();

        mockMvc.perform(get("/projetos/{projetoId}/habilidades", projetoId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetoHabilidadeService);
    }

    // ============================================
    // POST /projetos/{projetoId}/habilidades
    // ============================================

    @Test
    @WithUsuarioMock
    void vincular_deveRetornar201ComVinculoCriado() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID habilidadeId = UUID.randomUUID();
        VincularHabilidadeRequest request = new VincularHabilidadeRequest(habilidadeId, true);
        ProjetoHabilidadeResponse resposta = vinculoResponse(UUID.randomUUID(), habilidadeId, true);

        when(projetoHabilidadeService.vincular(eq(projetoId), any(VincularHabilidadeRequest.class), any(UUID.class)))
                .thenReturn(resposta);

        mockMvc.perform(post("/projetos/{projetoId}/habilidades", projetoId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.habilidade.id").value(habilidadeId.toString()))
                .andExpect(jsonPath("$.obrigatoria").value(true));

        verify(projetoHabilidadeService).vincular(eq(projetoId), any(VincularHabilidadeRequest.class), any(UUID.class));
    }

    @Test
    @WithUsuarioMock
    void vincular_quandoRegraDeNegocioViolada_deveRetornar400() throws Exception {
        UUID projetoId = UUID.randomUUID();
        VincularHabilidadeRequest request = new VincularHabilidadeRequest(UUID.randomUUID(), false);

        when(projetoHabilidadeService.vincular(eq(projetoId), any(VincularHabilidadeRequest.class), any(UUID.class)))
                .thenThrow(new RegraDeNegocioException("A habilidade 'Java' já está vinculada a este projeto."));

        mockMvc.perform(post("/projetos/{projetoId}/habilidades", projetoId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Violação de Regra de Negócio"))
                .andExpect(jsonPath("$.mensagem").value("A habilidade 'Java' já está vinculada a este projeto."));
    }

    @Test
    void vincular_semAutenticacao_deveRetornar403() throws Exception {
        UUID projetoId = UUID.randomUUID();
        VincularHabilidadeRequest request = new VincularHabilidadeRequest(UUID.randomUUID(), true);

        mockMvc.perform(post("/projetos/{projetoId}/habilidades", projetoId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetoHabilidadeService);
    }

    // ============================================
    // PATCH /projetos/{projetoId}/habilidades/{habilidadeId}
    // ============================================

    @Test
    @WithUsuarioMock
    void alterarObrigatoriedade_deveRetornar200ComVinculoAtualizado() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID habilidadeId = UUID.randomUUID();
        ProjetoHabilidadeResponse resposta = vinculoResponse(UUID.randomUUID(), habilidadeId, false);

        when(projetoHabilidadeService.alterarObrigatoriedade(eq(projetoId), eq(habilidadeId), anyBoolean(), any(UUID.class)))
                .thenReturn(resposta);

        mockMvc.perform(patch("/projetos/{projetoId}/habilidades/{habilidadeId}", projetoId, habilidadeId)
                        .param("obrigatoria", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obrigatoria").value(false));

        verify(projetoHabilidadeService).alterarObrigatoriedade(eq(projetoId), eq(habilidadeId), eq(false), any(UUID.class));
    }

    @Test
    void alterarObrigatoriedade_semAutenticacao_deveRetornar403() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID habilidadeId = UUID.randomUUID();

        mockMvc.perform(patch("/projetos/{projetoId}/habilidades/{habilidadeId}", projetoId, habilidadeId)
                        .param("obrigatoria", "true"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetoHabilidadeService);
    }

    // ============================================
    // DELETE /projetos/{projetoId}/habilidades/{habilidadeId}
    // ============================================

    @Test
    @WithUsuarioMock
    void desvincular_deveRetornar200ComMensagemDeSucesso() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID habilidadeId = UUID.randomUUID();

        doNothing().when(projetoHabilidadeService).desvincular(eq(projetoId), eq(habilidadeId), any(UUID.class));

        mockMvc.perform(delete("/projetos/{projetoId}/habilidades/{habilidadeId}", projetoId, habilidadeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Habilidade desvinculada do projeto com sucesso."));

        verify(projetoHabilidadeService).desvincular(eq(projetoId), eq(habilidadeId), any(UUID.class));
    }

    @Test
    void desvincular_semAutenticacao_deveRetornar403() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID habilidadeId = UUID.randomUUID();

        mockMvc.perform(delete("/projetos/{projetoId}/habilidades/{habilidadeId}", projetoId, habilidadeId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetoHabilidadeService);
    }
}
