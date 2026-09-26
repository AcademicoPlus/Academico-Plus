package com.unifapi.academico.controller;

import com.unifapi.academico.config.SecurityConfig;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.dto.mensagem.ConversaResponse;
import com.unifapi.academico.dto.projeto.ProjetoRefResponse;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.ChatProjetoService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ConversaController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class ConversaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatProjetoService chatProjetoService;

    @Test
    @WithUsuarioMock
    void listar_deveRetornarConversas() throws Exception {
        UUID projetoId = UUID.randomUUID();
        when(chatProjetoService.listarConversas(any(UUID.class))).thenReturn(List.of(
                new ConversaResponse(new ProjetoRefResponse(projetoId, "Projeto X", StatusProjeto.ABERTO), null, null, 3)));

        mockMvc.perform(get("/conversas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projeto.titulo").value("Projeto X"))
                .andExpect(jsonPath("$[0].naoLidas").value(3));
    }

    @Test
    @WithUsuarioMock
    void contarNaoLidas_deveRetornarTotal() throws Exception {
        when(chatProjetoService.contarNaoLidas(any(UUID.class))).thenReturn(5L);

        mockMvc.perform(get("/conversas/nao-lidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    @WithUsuarioMock
    void marcarComoLida_deveRetornar204() throws Exception {
        UUID projetoId = UUID.randomUUID();

        mockMvc.perform(put("/conversas/{projetoId}/lida", projetoId))
                .andExpect(status().isNoContent());

        verify(chatProjetoService).marcarComoLida(eq(projetoId), any(UUID.class));
    }

    @Test
    @WithUsuarioMock
    void marcarComoLida_deveRetornar400_quandoSemAcesso() throws Exception {
        UUID projetoId = UUID.randomUUID();
        doThrow(new RegraDeNegocioException("Você não tem acesso ao chat deste projeto."))
                .when(chatProjetoService).marcarComoLida(eq(projetoId), any(UUID.class));

        mockMvc.perform(put("/conversas/{projetoId}/lida", projetoId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/conversas"))
                .andExpect(status().isForbidden());
    }
}
