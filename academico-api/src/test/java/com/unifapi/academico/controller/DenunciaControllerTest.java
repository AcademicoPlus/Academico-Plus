package com.unifapi.academico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.domain.enums.CategoriaDenuncia;
import com.unifapi.academico.domain.enums.MedidaModeracao;
import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.enums.TipoAlvoDenuncia;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.denuncia.AlvoDenunciadoResponse;
import com.unifapi.academico.dto.denuncia.DenunciaResponse;
import com.unifapi.academico.dto.denuncia.DenunciarRequest;
import com.unifapi.academico.dto.denuncia.HistoricoModeracaoResponse;
import com.unifapi.academico.dto.denuncia.ResolverDenunciaRequest;
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

    private static UsuarioResumoResponse usuario(String nome) {
        return new UsuarioResumoResponse(
                UUID.randomUUID(), nome, "Sistemas de Informação", null, "ALUNO", 3, 4.0, 2);
    }

    private static DenunciaResponse respostaValida() {
        UsuarioResumoResponse responsavel = usuario("Avaliador Um");

        return new DenunciaResponse(
                UUID.randomUUID(),
                TipoAlvoDenuncia.AVALIACAO,
                new AlvoDenunciadoResponse(
                        UUID.randomUUID(), "Avaliação de Avaliador Um", "Comentário ofensivo", 1, responsavel),
                responsavel,
                usuario("Denunciante Um"),
                CategoriaDenuncia.CONTEUDO_OFENSIVO,
                "A avaliação contém xingamentos e ofensas pessoais.",
                StatusDenuncia.PENDENTE,
                null,
                new HistoricoModeracaoResponse(
                        0, 0, false, null, MedidaModeracao.ADVERTENCIA, "Sem histórico de sanções."),
                LocalDateTime.now(),
                null
        );
    }

    private static DenunciarRequest pedidoValido() {
        return new DenunciarRequest(
                CategoriaDenuncia.CONTEUDO_OFENSIVO, "A avaliação contém xingamentos e ofensas pessoais.");
    }

    // ---------------------------------------------------------------
    // POST /avaliacoes/{id}/denuncias — autenticado (qualquer role)
    // ---------------------------------------------------------------

    @Test
    @WithUsuarioMock
    void deveDenunciarAvaliacaoComSucessoERetornar201() throws Exception {
        UUID avaliacaoId = UUID.randomUUID();
        DenunciaResponse resposta = respostaValida();

        when(denunciaService.denunciarAvaliacao(eq(avaliacaoId), any(DenunciarRequest.class), any(UUID.class)))
                .thenReturn(resposta);

        mockMvc.perform(post("/avaliacoes/" + avaliacaoId + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedidoValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(resposta.id().toString()))
                .andExpect(jsonPath("$.tipoAlvo").value("AVALIACAO"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.motivo").value(resposta.motivo()));

        verify(denunciaService).denunciarAvaliacao(eq(avaliacaoId), any(DenunciarRequest.class), any(UUID.class));
    }

    @Test
    @WithUsuarioMock
    void deveRetornar400QuandoMotivoForBranco() throws Exception {
        DenunciarRequest request = new DenunciarRequest(CategoriaDenuncia.SPAM, "");

        mockMvc.perform(post("/avaliacoes/" + UUID.randomUUID() + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Erro de Validação"))
                .andExpect(jsonPath("$.erros[0].campo").value("motivo"));

        verifyNoInteractions(denunciaService);
    }

    @Test
    @WithUsuarioMock
    void deveRetornar400QuandoCategoriaNaoForInformada() throws Exception {
        DenunciarRequest request = new DenunciarRequest(null, "A avaliação contém ofensas pessoais.");

        mockMvc.perform(post("/avaliacoes/" + UUID.randomUUID() + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("categoria"));

        verifyNoInteractions(denunciaService);
    }

    @Test
    void deveRetornar403AoDenunciarSemAutenticacao() throws Exception {
        mockMvc.perform(post("/avaliacoes/" + UUID.randomUUID() + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedidoValido())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(denunciaService);
    }

    // ---------------------------------------------------------------
    // POST /projetos/{id}/denuncias e /usuarios/{id}/denuncias
    // ---------------------------------------------------------------

    @Test
    @WithUsuarioMock
    void deveDenunciarProjetoComSucessoERetornar201() throws Exception {
        UUID projetoId = UUID.randomUUID();
        when(denunciaService.denunciarProjeto(eq(projetoId), any(DenunciarRequest.class), any(UUID.class)))
                .thenReturn(respostaValida());

        mockMvc.perform(post("/projetos/" + projetoId + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedidoValido())))
                .andExpect(status().isCreated());

        verify(denunciaService).denunciarProjeto(eq(projetoId), any(DenunciarRequest.class), any(UUID.class));
    }

    @Test
    @WithUsuarioMock
    void deveDenunciarPerfilComSucessoERetornar201() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        when(denunciaService.denunciarPerfil(eq(usuarioId), any(DenunciarRequest.class), any(UUID.class)))
                .thenReturn(respostaValida());

        mockMvc.perform(post("/usuarios/" + usuarioId + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedidoValido())))
                .andExpect(status().isCreated());

        verify(denunciaService).denunciarPerfil(eq(usuarioId), any(DenunciarRequest.class), any(UUID.class));
    }

    @Test
    void deveRetornar403AoDenunciarPerfilSemAutenticacao() throws Exception {
        mockMvc.perform(post("/usuarios/" + UUID.randomUUID() + "/denuncias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedidoValido())))
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
        when(denunciaService.listarPendentes(any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/denuncias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.content[0].historicoResponsavel.medidaSugerida").value("ADVERTENCIA"));

        verify(denunciaService).listarPendentes(any(), any());
    }

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void deveRepassarOFiltroDeTipoDeAlvo() throws Exception {
        when(denunciaService.listarPendentes(eq(TipoAlvoDenuncia.PERFIL), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/denuncias").param("tipoAlvo", "PERFIL"))
                .andExpect(status().isOk());

        verify(denunciaService).listarPendentes(eq(TipoAlvoDenuncia.PERFIL), any());
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
    // PATCH /denuncias/{id}/resolver — somente ADMIN
    // ---------------------------------------------------------------

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void deveResolverDenunciaComoAdmin() throws Exception {
        UUID denunciaId = UUID.randomUUID();
        DenunciaResponse resposta = respostaValida();
        ResolverDenunciaRequest request = new ResolverDenunciaRequest(
                MedidaModeracao.ADVERTENCIA, null, "Comentário ofensivo a colega.");

        when(denunciaService.resolver(eq(denunciaId), any(ResolverDenunciaRequest.class), any(UUID.class)))
                .thenReturn(resposta);

        mockMvc.perform(patch("/denuncias/" + denunciaId + "/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resposta.id().toString()));

        verify(denunciaService).resolver(eq(denunciaId), any(ResolverDenunciaRequest.class), any(UUID.class));
    }

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void deveRetornar400QuandoMedidaNaoForInformada() throws Exception {
        ResolverDenunciaRequest request = new ResolverDenunciaRequest(null, null, "Qualquer coisa");

        mockMvc.perform(patch("/denuncias/" + UUID.randomUUID() + "/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("medida"));

        verifyNoInteractions(denunciaService);
    }

    @Test
    @WithUsuarioMock(role = "ADMIN")
    void deveRetornar400QuandoPrazoDeSuspensaoForAbsurdo() throws Exception {
        ResolverDenunciaRequest request = new ResolverDenunciaRequest(
                MedidaModeracao.SUSPENSAO, 5000, "Conteúdo grave.");

        mockMvc.perform(patch("/denuncias/" + UUID.randomUUID() + "/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("diasSuspensao"));

        verifyNoInteractions(denunciaService);
    }

    @Test
    @WithUsuarioMock(role = "ALUNO")
    void deveRetornar403AoResolverComoAluno() throws Exception {
        ResolverDenunciaRequest request = new ResolverDenunciaRequest(MedidaModeracao.ARQUIVAR, null, null);

        mockMvc.perform(patch("/denuncias/" + UUID.randomUUID() + "/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(denunciaService);
    }

    @Test
    void deveRetornar403AoResolverSemAutenticacao() throws Exception {
        ResolverDenunciaRequest request = new ResolverDenunciaRequest(MedidaModeracao.ARQUIVAR, null, null);

        mockMvc.perform(patch("/denuncias/" + UUID.randomUUID() + "/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(denunciaService);
    }
}
