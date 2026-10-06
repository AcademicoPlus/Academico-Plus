package com.unifapi.academico.controller;

import com.unifapi.academico.dto.usuario.CursoResponse;
import com.unifapi.academico.security.GlobalRateLimitFilter;
import com.unifapi.academico.security.JwtAuthenticationFilter;
import com.unifapi.academico.service.CursoService;
import com.unifapi.academico.config.SecurityConfig;
import com.unifapi.academico.support.SecurityTestConfig;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@code GET /cursos} é {@code permitAll} em SecurityConfig — precisa ficar acessível
 * antes do login para alimentar o dropdown da tela de cadastro.
 */
@WebMvcTest(
        controllers = CursoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, GlobalRateLimitFilter.class}
        )
)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class CursoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CursoService cursoService;

    @Test
    void listar_deveRetornar200ComListaDeCursos_semAutenticacao() throws Exception {
        CursoResponse curso = new CursoResponse(UUID.randomUUID(), "Ciência da Computação", LocalDateTime.now());
        when(cursoService.listarAtivos()).thenReturn(List.of(curso));

        mockMvc.perform(get("/cursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Ciência da Computação"));

        verify(cursoService).listarAtivos();
    }

    @Test
    void listar_deveRetornarListaVazia_quandoNaoHaCursosAtivos() throws Exception {
        when(cursoService.listarAtivos()).thenReturn(List.of());

        mockMvc.perform(get("/cursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
