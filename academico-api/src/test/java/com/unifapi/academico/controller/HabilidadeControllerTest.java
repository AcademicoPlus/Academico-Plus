package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.habilidade.AtualizarHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.CriarHabilidadeRequest;
import com.unifapi.academico.dto.habilidade.HabilidadeResponse;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.HabilidadeService;
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

import java.util.List;
import java.util.UUID;

import static com.unifapi.academico.support.EntityFixtures.habilidade;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test do {@link HabilidadeController}: leitura liberada a qualquer usuário
 * autenticado, escrita restrita a ADMIN (ver {@code @PreAuthorize} no controller).
 */
@WebMvcTest(
        controllers = HabilidadeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class HabilidadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HabilidadeService habilidadeService;

    private HabilidadeResponse respostaPadrao() {
        return HabilidadeResponse.fromEntity(habilidade("Java", "Backend"));
    }

    // ============================================
    // LEITURA
    // ============================================

    @Test
    @WithUsuarioMock
    void listar_deveRetornarPaginaDeHabilidades_quandoAutenticado() throws Exception {
        Page<HabilidadeResponse> pagina = new PageImpl<>(List.of(respostaPadrao()), PageRequest.of(0, 20), 1);
        when(habilidadeService.listar(any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/habilidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Java"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/habilidades"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void listarCategorias_deveRetornarCategorias_quandoAutenticado() throws Exception {
        when(habilidadeService.listarCategorias()).thenReturn(List.of("Backend", "Frontend"));

        mockMvc.perform(get("/habilidades/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Backend"))
                .andExpect(jsonPath("$[1]").value("Frontend"));
    }

    @Test
    @WithUsuarioMock
    void buscarPorId_deveRetornarHabilidade_quandoAutenticado() throws Exception {
        UUID id = UUID.randomUUID();
        when(habilidadeService.buscarPorId(eq(id))).thenReturn(respostaPadrao());

        mockMvc.perform(get("/habilidades/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Java"))
                .andExpect(jsonPath("$.categoria").value("Backend"));
    }

    // ============================================
    // CRIAR (ADMIN)
    // ============================================

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void criar_devePermitir_quandoAdmin() throws Exception {
        CriarHabilidadeRequest request = new CriarHabilidadeRequest("Java", "Backend", "Linguagem de programação");
        when(habilidadeService.criar(any(CriarHabilidadeRequest.class))).thenReturn(respostaPadrao());

        mockMvc.perform(post("/habilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Java"));
    }

    @Test
    @WithUsuarioMock
    void criar_deveRetornar403_quandoAluno() throws Exception {
        CriarHabilidadeRequest request = new CriarHabilidadeRequest("Java", "Backend", "Linguagem de programação");

        mockMvc.perform(post("/habilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(habilidadeService, never()).criar(any());
    }

    @Test
    void criar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        CriarHabilidadeRequest request = new CriarHabilidadeRequest("Java", "Backend", "Linguagem de programação");

        mockMvc.perform(post("/habilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void criar_deveRetornar400_quandoNomeEmBranco() throws Exception {
        CriarHabilidadeRequest request = new CriarHabilidadeRequest("", "Backend", "Descrição válida");

        mockMvc.perform(post("/habilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"))
                .andExpect(jsonPath("$.erros[*].campo", hasItem("nome")));

        verify(habilidadeService, never()).criar(any());
    }

    // ============================================
    // ATUALIZAR (ADMIN)
    // ============================================

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void atualizar_devePermitir_quandoAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        AtualizarHabilidadeRequest request = new AtualizarHabilidadeRequest("Java Avançado", "Backend", "Descrição");
        when(habilidadeService.atualizar(eq(id), any(AtualizarHabilidadeRequest.class))).thenReturn(respostaPadrao());

        mockMvc.perform(put("/habilidades/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Java"));
    }

    @Test
    @WithUsuarioMock
    void atualizar_deveRetornar403_quandoAluno() throws Exception {
        UUID id = UUID.randomUUID();
        AtualizarHabilidadeRequest request = new AtualizarHabilidadeRequest("Java Avançado", "Backend", "Descrição");

        mockMvc.perform(put("/habilidades/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(habilidadeService, never()).atualizar(any(), any());
    }

    @Test
    void atualizar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        UUID id = UUID.randomUUID();
        AtualizarHabilidadeRequest request = new AtualizarHabilidadeRequest("Java Avançado", "Backend", "Descrição");

        mockMvc.perform(put("/habilidades/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============================================
    // REMOVER (ADMIN)
    // ============================================

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void remover_devePermitir_quandoAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(habilidadeService).remover(id);

        mockMvc.perform(delete("/habilidades/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true));

        verify(habilidadeService).remover(id);
    }

    @Test
    @WithUsuarioMock
    void remover_deveRetornar403_quandoAluno() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/habilidades/{id}", id))
                .andExpect(status().isForbidden());

        verify(habilidadeService, never()).remover(any());
    }

    @Test
    void remover_deveRetornar403_quandoNaoAutenticado() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/habilidades/{id}", id))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void remover_deveRetornar400_quandoHabilidadeEmUso() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RegraDeNegocioException("A habilidade 'Java' está em uso (2 projeto(s) e 0 usuário(s)) e não pode ser removida."))
                .when(habilidadeService).remover(id);

        mockMvc.perform(delete("/habilidades/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Violação de Regra de Negócio"))
                .andExpect(jsonPath("$.mensagem").value(
                        "A habilidade 'Java' está em uso (2 projeto(s) e 0 usuário(s)) e não pode ser removida."));
    }

    @SuppressWarnings("unused")
    private static MensagemResponse mensagemResponseParaReferencia() {
        // Mantido apenas para deixar explícito o shape do retorno de `remover` (evita import não usado ficar sem propósito).
        return new MensagemResponse(true, "Habilidade removida com sucesso.");
    }
}
