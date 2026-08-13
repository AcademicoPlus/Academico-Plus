package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.membro.AdicionarMembroRequest;
import com.unifapi.academico.dto.membro.AtualizarFuncaoMembroRequest;
import com.unifapi.academico.dto.membro.ProjetoMembroResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.ProjetoMembroService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ProjetoMembroController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class ProjetoMembroControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProjetoMembroService projetoMembroService;

    private static ProjetoMembroResponse membroResponse(UUID id, String funcao) {
        UsuarioResumoResponse usuario = new UsuarioResumoResponse(
                UUID.randomUUID(), "Membro Teste", "membro@unifapi.edu.br",
                null, null, "ALUNO", null, null, 0);
        return new ProjetoMembroResponse(id, null, usuario, funcao, LocalDateTime.now());
    }

    // ============================================
    // GET /projetos/{projetoId}/membros
    // ============================================

    @Test
    @WithUsuarioMock
    void listar_deveRetornarMembrosDoProjeto() throws Exception {
        UUID projetoId = UUID.randomUUID();
        ProjetoMembroResponse resposta = membroResponse(UUID.randomUUID(), "Desenvolvedor");

        when(projetoMembroService.listarPorProjeto(projetoId)).thenReturn(List.of(resposta));

        mockMvc.perform(get("/projetos/{projetoId}/membros", projetoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].funcao").value("Desenvolvedor"));

        verify(projetoMembroService).listarPorProjeto(projetoId);
    }

    @Test
    void listar_semAutenticacao_deveRetornar403() throws Exception {
        UUID projetoId = UUID.randomUUID();

        mockMvc.perform(get("/projetos/{projetoId}/membros", projetoId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetoMembroService);
    }

    // ============================================
    // GET /projetos/{projetoId}/membros/{membroId}
    // ============================================

    @Test
    @WithUsuarioMock
    void buscarPorId_deveRetornarMembro() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID membroId = UUID.randomUUID();
        ProjetoMembroResponse resposta = membroResponse(membroId, "Designer");

        when(projetoMembroService.buscarPorId(projetoId, membroId)).thenReturn(resposta);

        mockMvc.perform(get("/projetos/{projetoId}/membros/{membroId}", projetoId, membroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(membroId.toString()))
                .andExpect(jsonPath("$.funcao").value("Designer"));

        verify(projetoMembroService).buscarPorId(projetoId, membroId);
    }

    @Test
    @WithUsuarioMock
    void buscarPorId_quandoMembroNaoEncontrado_deveRetornar404() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID membroId = UUID.randomUUID();

        when(projetoMembroService.buscarPorId(projetoId, membroId))
                .thenThrow(new RecursoNaoEncontradoException("Este membro não pertence ao projeto informado."));

        mockMvc.perform(get("/projetos/{projetoId}/membros/{membroId}", projetoId, membroId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.mensagem").value("Este membro não pertence ao projeto informado."));
    }

    @Test
    void buscarPorId_semAutenticacao_deveRetornar403() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID membroId = UUID.randomUUID();

        mockMvc.perform(get("/projetos/{projetoId}/membros/{membroId}", projetoId, membroId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetoMembroService);
    }

    // ============================================
    // POST /projetos/{projetoId}/membros
    // ============================================

    @Test
    @WithUsuarioMock
    void adicionar_deveRetornar201ComMembroCriado() throws Exception {
        UUID projetoId = UUID.randomUUID();
        AdicionarMembroRequest request = new AdicionarMembroRequest(UUID.randomUUID(), "Tester");
        ProjetoMembroResponse resposta = membroResponse(UUID.randomUUID(), "Tester");

        when(projetoMembroService.adicionar(eq(projetoId), any(AdicionarMembroRequest.class), any(UUID.class)))
                .thenReturn(resposta);

        mockMvc.perform(post("/projetos/{projetoId}/membros", projetoId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.funcao").value("Tester"));

        verify(projetoMembroService).adicionar(eq(projetoId), any(AdicionarMembroRequest.class), any(UUID.class));
    }

    @Test
    void adicionar_semAutenticacao_deveRetornar403() throws Exception {
        UUID projetoId = UUID.randomUUID();
        AdicionarMembroRequest request = new AdicionarMembroRequest(UUID.randomUUID(), "Tester");

        mockMvc.perform(post("/projetos/{projetoId}/membros", projetoId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetoMembroService);
    }

    // ============================================
    // PATCH /projetos/{projetoId}/membros/{membroId}/funcao
    // ============================================

    @Test
    @WithUsuarioMock
    void atualizarFuncao_deveRetornar200ComFuncaoAtualizada() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID membroId = UUID.randomUUID();
        AtualizarFuncaoMembroRequest request = new AtualizarFuncaoMembroRequest("Líder Técnico");
        ProjetoMembroResponse resposta = membroResponse(membroId, "Líder Técnico");

        when(projetoMembroService.atualizarFuncao(eq(projetoId), eq(membroId), any(AtualizarFuncaoMembroRequest.class), any(UUID.class)))
                .thenReturn(resposta);

        mockMvc.perform(patch("/projetos/{projetoId}/membros/{membroId}/funcao", projetoId, membroId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcao").value("Líder Técnico"));

        verify(projetoMembroService).atualizarFuncao(eq(projetoId), eq(membroId), any(AtualizarFuncaoMembroRequest.class), any(UUID.class));
    }

    @Test
    void atualizarFuncao_semAutenticacao_deveRetornar403() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID membroId = UUID.randomUUID();
        AtualizarFuncaoMembroRequest request = new AtualizarFuncaoMembroRequest("Líder Técnico");

        mockMvc.perform(patch("/projetos/{projetoId}/membros/{membroId}/funcao", projetoId, membroId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetoMembroService);
    }

    // ============================================
    // DELETE /projetos/{projetoId}/membros/{membroId}
    // ============================================

    @Test
    @WithUsuarioMock
    void remover_deveRetornar200ComMensagemDeSucesso() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID membroId = UUID.randomUUID();

        doNothing().when(projetoMembroService).remover(eq(projetoId), eq(membroId), any(UUID.class));

        mockMvc.perform(delete("/projetos/{projetoId}/membros/{membroId}", projetoId, membroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Membro removido do projeto com sucesso."));

        verify(projetoMembroService).remover(eq(projetoId), eq(membroId), any(UUID.class));
    }

    @Test
    @WithUsuarioMock
    void remover_quandoNemDonoNemProprioMembro_deveRetornar400() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID membroId = UUID.randomUUID();

        doThrow(new RegraDeNegocioException("Você não tem permissão para remover este membro do projeto."))
                .when(projetoMembroService).remover(eq(projetoId), eq(membroId), any(UUID.class));

        mockMvc.perform(delete("/projetos/{projetoId}/membros/{membroId}", projetoId, membroId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Violação de Regra de Negócio"))
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para remover este membro do projeto."));
    }

    @Test
    void remover_semAutenticacao_deveRetornar403() throws Exception {
        UUID projetoId = UUID.randomUUID();
        UUID membroId = UUID.randomUUID();

        mockMvc.perform(delete("/projetos/{projetoId}/membros/{membroId}", projetoId, membroId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetoMembroService);
    }
}
