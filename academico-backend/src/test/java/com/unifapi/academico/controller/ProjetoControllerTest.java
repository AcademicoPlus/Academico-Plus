package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.dto.projeto.AlterarStatusProjetoRequest;
import com.unifapi.academico.dto.projeto.AtualizarProjetoRequest;
import com.unifapi.academico.dto.projeto.CriarProjetoRequest;
import com.unifapi.academico.dto.projeto.ProjetoResponse;
import com.unifapi.academico.dto.projeto.ProjetoResumoResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.ProjetoService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Todos os endpoints exigem autenticação (regra de posse — "só o criador edita" —
 * é aplicada dentro do service, mockado aqui; a este nível só se verifica o
 * repasse do id do usuário autenticado e o mapeamento de exceções para HTTP).
 */
@WebMvcTest(
        controllers = ProjetoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class ProjetoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjetoService projetoService;

    private ProjetoResponse respostaDetalhada() {
        return new ProjetoResponse(UUID.randomUUID(), null, "Projeto de robótica educacional",
                "Descrição com pelo menos vinte caracteres.", null, StatusProjeto.ABERTO, 3, 0, 3, true,
                null, true, LocalDateTime.now(), LocalDateTime.now(), List.of(), 0, 0);
    }

    private ProjetoResumoResponse respostaResumo() {
        return new ProjetoResumoResponse(UUID.randomUUID(), null, "Projeto de robótica educacional",
                "Descrição com pelo menos vinte caracteres.", null, StatusProjeto.ABERTO, List.of(), 3, 0, null);
    }

    // ============================================
    // POST /projetos
    // ============================================

    @Test
    @WithUsuarioMock
    void criar_deveRetornar201_quandoDadosValidos() throws Exception {
        CriarProjetoRequest request = new CriarProjetoRequest(
                "Projeto de robótica educacional", "Descrição com pelo menos vinte caracteres.", 3, null, null);
        when(projetoService.criar(any(CriarProjetoRequest.class), any(UUID.class))).thenReturn(respostaDetalhada());

        mockMvc.perform(post("/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Projeto de robótica educacional"));
    }

    @Test
    @WithUsuarioMock
    void criar_deveRetornar400_quandoTituloMuitoCurto() throws Exception {
        CriarProjetoRequest request = new CriarProjetoRequest(
                "Ab", "Descrição com pelo menos vinte caracteres.", 3, null, null);

        mockMvc.perform(post("/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"));

        verify(projetoService, never()).criar(any(), any());
    }

    @Test
    void criar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        CriarProjetoRequest request = new CriarProjetoRequest(
                "Projeto de robótica educacional", "Descrição com pelo menos vinte caracteres.", 3, null, null);

        mockMvc.perform(post("/projetos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============================================
    // PUT /projetos/{id}
    // ============================================

    @Test
    @WithUsuarioMock
    void atualizar_deveRetornar200_quandoUsuarioEhCriador() throws Exception {
        UUID id = UUID.randomUUID();
        AtualizarProjetoRequest request = new AtualizarProjetoRequest(
                "Titulo atualizado", "Descrição com pelo menos vinte caracteres.", 3, null);
        when(projetoService.atualizar(eq(id), any(AtualizarProjetoRequest.class), any(UUID.class)))
                .thenReturn(respostaDetalhada());

        mockMvc.perform(put("/projetos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void atualizar_deveRetornar400_quandoNaoEhCriador() throws Exception {
        UUID id = UUID.randomUUID();
        AtualizarProjetoRequest request = new AtualizarProjetoRequest(
                "Titulo atualizado", "Descrição com pelo menos vinte caracteres.", 3, null);
        when(projetoService.atualizar(eq(id), any(AtualizarProjetoRequest.class), any(UUID.class)))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        mockMvc.perform(put("/projetos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para alterar este projeto."));
    }

    // ============================================
    // PATCH /projetos/{id}/status
    // ============================================

    @Test
    @WithUsuarioMock
    void alterarStatus_deveRetornar200_quandoTransicaoValida() throws Exception {
        UUID id = UUID.randomUUID();
        AlterarStatusProjetoRequest request = new AlterarStatusProjetoRequest(StatusProjeto.EM_ANDAMENTO, null);
        when(projetoService.alterarStatus(eq(id), any(AlterarStatusProjetoRequest.class), any(UUID.class)))
                .thenReturn(respostaDetalhada());

        mockMvc.perform(patch("/projetos/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void alterarStatus_deveRetornar400_quandoProjetoJaConcluido() throws Exception {
        UUID id = UUID.randomUUID();
        AlterarStatusProjetoRequest request = new AlterarStatusProjetoRequest(StatusProjeto.CANCELADO, null);
        when(projetoService.alterarStatus(eq(id), any(AlterarStatusProjetoRequest.class), any(UUID.class)))
                .thenThrow(new RegraDeNegocioException("Projetos com status Concluído não podem ter o status alterado."));

        mockMvc.perform(patch("/projetos/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ============================================
    // DELETE /projetos/{id}
    // ============================================

    @Test
    @WithUsuarioMock
    void inativar_deveRetornar200_quandoUsuarioEhCriador() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(projetoService).inativar(eq(id), any(UUID.class));

        mockMvc.perform(delete("/projetos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true));
    }

    @Test
    void inativar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(delete("/projetos/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // ============================================
    // POST /projetos/{id}/banner
    // ============================================

    @Test
    @WithUsuarioMock
    void uploadBanner_deveRetornar200_quandoArquivoValido() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile arquivo = new MockMultipartFile("banner", "banner.png", "image/png", "conteudo".getBytes());
        when(projetoService.atualizarBanner(eq(id), any(UUID.class), any())).thenReturn(respostaDetalhada());

        mockMvc.perform(multipart("/projetos/{id}/banner", id).file(arquivo))
                .andExpect(status().isOk());
    }

    @Test
    void uploadBanner_deveRetornar403_quandoNaoAutenticado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("banner", "banner.png", "image/png", "conteudo".getBytes());

        mockMvc.perform(multipart("/projetos/{id}/banner", UUID.randomUUID()).file(arquivo))
                .andExpect(status().isForbidden());
    }

    // ============================================
    // LEITURA
    // ============================================

    @Test
    @WithUsuarioMock
    void listar_deveRetornar200_comFiltros() throws Exception {
        Page<ProjetoResumoResponse> pagina = new PageImpl<>(List.of(respostaResumo()), PageRequest.of(0, 10), 1);
        when(projetoService.listar(any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/projetos").param("status", "ABERTO").param("busca", "robotica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ABERTO"));
    }

    @Test
    void listar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/projetos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void listarMeusProjetos_deveRetornar200() throws Exception {
        Page<ProjetoResumoResponse> pagina = new PageImpl<>(List.of(respostaResumo()), PageRequest.of(0, 10), 1);
        when(projetoService.listarPorCriador(any(UUID.class), any())).thenReturn(pagina);

        mockMvc.perform(get("/projetos/meus-projetos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void listarParticipando_deveRetornar200() throws Exception {
        Page<ProjetoResumoResponse> pagina = new PageImpl<>(List.of(respostaResumo()), PageRequest.of(0, 10), 1);
        when(projetoService.listarParticipando(any(UUID.class), any())).thenReturn(pagina);

        mockMvc.perform(get("/projetos/participando"))
                .andExpect(status().isOk());
    }

    @Test
    @WithUsuarioMock
    void buscarPorId_deveRetornar200_quandoAutenticado() throws Exception {
        UUID id = UUID.randomUUID();
        when(projetoService.buscarPorId(eq(id))).thenReturn(respostaDetalhada());

        mockMvc.perform(get("/projetos/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/projetos/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUsuarioMock
    void buscarPorId_deveRetornar404_quandoProjetoNaoEncontrado() throws Exception {
        UUID id = UUID.randomUUID();
        when(projetoService.buscarPorId(eq(id)))
                .thenThrow(new RecursoNaoEncontradoException("Projeto não encontrado."));

        mockMvc.perform(get("/projetos/{id}", id))
                .andExpect(status().isNotFound());
    }
}
