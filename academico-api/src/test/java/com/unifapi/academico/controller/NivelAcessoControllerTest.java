package com.unifapi.academico.controller;

import com.unifapi.academico.dto.usuario.NivelAcessoResponse;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.NivelAcessoService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** {@code GET /niveis-acesso} é {@code @PreAuthorize("hasRole('ADMIN')")}. */
@WebMvcTest(
        controllers = NivelAcessoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class NivelAcessoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NivelAcessoService nivelAcessoService;

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void listar_devePermitir_quandoAdmin() throws Exception {
        NivelAcessoResponse resposta = new NivelAcessoResponse(UUID.randomUUID(), "ALUNO", "Aluno da instituição");
        when(nivelAcessoService.listarTodos()).thenReturn(List.of(resposta));

        mockMvc.perform(get("/niveis-acesso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("ALUNO"));
    }

    @Test
    @WithUsuarioMock
    void listar_deveRetornar403_quandoAluno() throws Exception {
        mockMvc.perform(get("/niveis-acesso"))
                .andExpect(status().isForbidden());

        verify(nivelAcessoService, never()).listarTodos();
    }

    @Test
    void listar_deveRetornar403_quandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/niveis-acesso"))
                .andExpect(status().isForbidden());
    }
}
