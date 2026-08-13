package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.dto.avaliacao.AvaliacaoResponse;
import com.unifapi.academico.dto.avaliacao.AvaliarRequest;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.projeto.ProjetoRefResponse;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.AvaliacaoService;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ProjetoAvaliacaoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class ProjetoAvaliacaoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AvaliacaoService avaliacaoService;

    private static String url(UUID projetoId) {
        return "/projetos/" + projetoId + "/avaliacoes";
    }

    private static AvaliacaoResponse respostaValida() {
        UsuarioResumoResponse avaliador = new UsuarioResumoResponse(
                UUID.randomUUID(), "Fulano de Tal", "fulano@unifapi.edu.br",
                "Ciência da Computação", null, "ALUNO", 5, 4.5, 3
        );
        ProjetoRefResponse projeto = new ProjetoRefResponse(UUID.randomUUID(), "Projeto X", StatusProjeto.CONCLUIDO);
        return new AvaliacaoResponse(
                UUID.randomUUID(), projeto, avaliador, 5, "Ótimo trabalho em equipe", LocalDateTime.now()
        );
    }

    @Test
    @WithUsuarioMock
    void deveAvaliarComSucessoERetornar201() throws Exception {
        UUID projetoId = UUID.randomUUID();
        AvaliarRequest request = new AvaliarRequest(UUID.randomUUID(), 5, "Ótimo trabalho em equipe");
        AvaliacaoResponse resposta = respostaValida();

        when(avaliacaoService.avaliar(eq(projetoId), any(AvaliarRequest.class), any(UUID.class)))
                .thenReturn(resposta);

        mockMvc.perform(post(url(projetoId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(resposta.id().toString()))
                .andExpect(jsonPath("$.nota").value(5))
                .andExpect(jsonPath("$.comentario").value("Ótimo trabalho em equipe"));

        verify(avaliacaoService).avaliar(eq(projetoId), any(AvaliarRequest.class), any(UUID.class));
    }

    @Test
    @WithUsuarioMock
    void deveRetornar400QuandoNotaForaDoIntervaloPermitido() throws Exception {
        UUID projetoId = UUID.randomUUID();
        AvaliarRequest request = new AvaliarRequest(UUID.randomUUID(), 0, "Comentário qualquer");

        mockMvc.perform(post(url(projetoId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"))
                .andExpect(jsonPath("$.erros[0].campo").value("nota"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Nota mínima é 1"));

        verifyNoInteractions(avaliacaoService);
    }

    @Test
    @WithUsuarioMock
    void deveRetornar400QuandoRegraDeNegocioForViolada() throws Exception {
        UUID projetoId = UUID.randomUUID();
        AvaliarRequest request = new AvaliarRequest(UUID.randomUUID(), 4, null);

        when(avaliacaoService.avaliar(eq(projetoId), any(AvaliarRequest.class), any(UUID.class)))
                .thenThrow(new RegraDeNegocioException("Você não pode avaliar a si mesmo."));

        mockMvc.perform(post(url(projetoId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Violação de Regra de Negócio"))
                .andExpect(jsonPath("$.mensagem").value("Você não pode avaliar a si mesmo."));
    }

    @Test
    void deveRetornar403QuandoNaoAutenticado() throws Exception {
        UUID projetoId = UUID.randomUUID();
        AvaliarRequest request = new AvaliarRequest(UUID.randomUUID(), 5, "Comentário");

        mockMvc.perform(post(url(projetoId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(avaliacaoService);
    }
}
