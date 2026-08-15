package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.avaliacao.DenunciaResponse;
import com.unifapi.academico.dto.avaliacao.DenunciarAvaliacaoRequest;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.DenunciaService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DenunciaController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class DenunciaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private DenunciaService denunciaService;

    private static DenunciaResponse respostaValida() {
        UsuarioResumoResponse avaliador = new UsuarioResumoResponse(
                UUID.randomUUID(), "Avaliador Um",
                "Sistemas de Informação", null, "ALUNO", 3, 4.0, 2
        );
        UsuarioResumoResponse denunciante = new UsuarioResumoResponse(
                UUID.randomUUID(), "Denunciante Um",
                "Engenharia de Software", null, "ALUNO", 4, 3.5, 1
        );
        return new DenunciaResponse(
                UUID.randomUUID(), UUID.randomUUID(), avaliador, 1, "Comentário ofensivo",
                denunciante, "A avaliação contém xingamentos e ofensas pessoais.",
                StatusDenuncia.PENDENTE, LocalDateTime.now(), null
        );
    }

    // ---------------------------------------------------------------
    // POST /avaliacoes/{avaliacaoId}/denuncias — autenticado (qualquer role)
    // ---------------------------------------------------------------

    @Test
    @WithUsuarioMock
    void deveDenunciarComSucessoERetornar201() throws Exception {
        UUID avaliacaoId = UUID.randomUUID();
        DenunciarAvaliacaoRequest request = new DenunciarAvaliacaoRequest(
                "A avaliação contém xingamentos e ofensas pessoais.");
        DenunciaResponse resposta = respostaValida();

        when(denunciaService.denunciar(eq(avaliacaoId), any(DenunciarAvaliacaoRequest.class), any(UUID.class)))
                .thenReturn(resposta);

        mockMvc.perform(post("/avaliacoes/" + avaliacaoId + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(resposta.id().toString()))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.motivo").value(resposta.motivo()));

        verify(denunciaService).denunciar(eq(avaliacaoId), any(DenunciarAvaliacaoRequest.class), any(UUID.class));
    }

    @Test
    @WithUsuarioMock
    void deveRetornar400QuandoMotivoForBranco() throws Exception {
        UUID avaliacaoId = UUID.randomUUID();
        DenunciarAvaliacaoRequest request = new DenunciarAvaliacaoRequest("");

        mockMvc.perform(post("/avaliacoes/" + avaliacaoId + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"))
                .andExpect(jsonPath("$.erros[0].campo").value("motivo"));

        verifyNoInteractions(denunciaService);
    }

    @Test
    void deveRetornar403AoDenunciarSemAutenticacao() throws Exception {
        UUID avaliacaoId = UUID.randomUUID();
        DenunciarAvaliacaoRequest request = new DenunciarAvaliacaoRequest(
                "A avaliação contém xingamentos e ofensas pessoais.");

        mockMvc.perform(post("/avaliacoes/" + avaliacaoId + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(denunciaService);
    }

    // ---------------------------------------------------------------
    // GET /denuncias — somente ADMIN
    // ---------------------------------------------------------------

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void deveListarPendentesComoAdmin() throws Exception {
        Page<DenunciaResponse> pagina = new PageImpl<>(List.of(respostaValida()), PageRequest.of(0, 10), 1);
        when(denunciaService.listarPendentes(any())).thenReturn(pagina);

        mockMvc.perform(get("/denuncias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDENTE"));

        verify(denunciaService).listarPendentes(any());
    }

    @Test
    @WithUsuarioMock(role = "ALUNO")
    void deveRetornar403AoListarPendentesComoAluno() throws Exception {
        mockMvc.perform(get("/denuncias"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(denunciaService);
    }

    @Test
    void deveRetornar403AoListarPendentesSemAutenticacao() throws Exception {
        mockMvc.perform(get("/denuncias"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(denunciaService);
    }

    // ---------------------------------------------------------------
    // PATCH /denuncias/{id}/resolver?procedente= — somente ADMIN
    // ---------------------------------------------------------------

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void deveResolverDenunciaComoAdmin() throws Exception {
        UUID denunciaId = UUID.randomUUID();
        DenunciaResponse resposta = respostaValida();

        when(denunciaService.resolver(eq(denunciaId), eq(true), any(UUID.class))).thenReturn(resposta);

        mockMvc.perform(patch("/denuncias/" + denunciaId + "/resolver")
                        .param("procedente", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resposta.id().toString()));

        verify(denunciaService).resolver(eq(denunciaId), eq(true), any(UUID.class));
    }

    @Test
    @WithUsuarioMock(role = "ALUNO")
    void deveRetornar403AoResolverComoAluno() throws Exception {
        UUID denunciaId = UUID.randomUUID();

        mockMvc.perform(patch("/denuncias/" + denunciaId + "/resolver")
                        .param("procedente", "true"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(denunciaService);
    }

    @Test
    void deveRetornar403AoResolverSemAutenticacao() throws Exception {
        UUID denunciaId = UUID.randomUUID();

        mockMvc.perform(patch("/denuncias/" + denunciaId + "/resolver")
                        .param("procedente", "true"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(denunciaService);
    }
}
