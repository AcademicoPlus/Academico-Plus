package com.unifapi.academico.controller;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.dto.projeto.ProjetoResumoResponse;
import com.unifapi.academico.dto.recomendacao.ProjetoRecomendadoResponse;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.RecomendacaoService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = RecomendacaoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class RecomendacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecomendacaoService recomendacaoService;

    private ProjetoResumoResponse projetoResumo() {
        return new ProjetoResumoResponse(UUID.randomUUID(), null, "Projeto de robótica educacional",
                "Descrição com pelo menos vinte caracteres.", null, StatusProjeto.ABERTO, List.of(), 3, 0, null);
    }

    @Test
    @WithUsuarioMock
    void recomendarProjetos_deveRetornar200_semLimiteInformado() throws Exception {
        ProjetoRecomendadoResponse recomendado = new ProjetoRecomendadoResponse(projetoResumo(), 0.75, List.of());
        when(recomendacaoService.recomendarProjetos(any(UUID.class), isNull())).thenReturn(List.of(recomendado));

        mockMvc.perform(get("/recomendacoes/projetos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].compatibilidade").value(0.75));
    }

    @Test
    @WithUsuarioMock
    void recomendarProjetos_deveRetornar200_comLimiteInformado() throws Exception {
        when(recomendacaoService.recomendarProjetos(any(UUID.class), eq(5))).thenReturn(List.of());

        mockMvc.perform(get("/recomendacoes/projetos").param("limite", "5"))
                .andExpect(status().isOk());

        verify(recomendacaoService).recomendarProjetos(any(UUID.class), eq(5));
    }

    @Test
    void recomendarProjetos_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/recomendacoes/projetos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void recomendarCandidatos_deveRetornar200_quandoCriador() throws Exception {
        UUID projetoId = UUID.randomUUID();
        when(recomendacaoService.recomendarCandidatos(eq(projetoId), any(UUID.class), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/recomendacoes/projetos/{projetoId}/candidatos", projetoId))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void recomendarCandidatos_deveRetornar400_quandoNaoEhCriador() throws Exception {
        UUID projetoId = UUID.randomUUID();
        when(recomendacaoService.recomendarCandidatos(eq(projetoId), any(UUID.class), isNull()))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        mockMvc.perform(get("/recomendacoes/projetos/{projetoId}/candidatos", projetoId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recomendarCandidatos_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/recomendacoes/projetos/{projetoId}/candidatos", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
