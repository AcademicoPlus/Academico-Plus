package com.unifapi.academico.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (sem contexto Spring) para {@link GlobalRateLimitFilter}.
 *
 * <p>O filtro recebe o limite de requisições por janela diretamente pelo construtor
 * (o valor que, em produção, o Spring injeta via {@code @Value("${app.rate-limit.requisicoes-por-minuto:120}")}),
 * então não é necessário usar {@code ReflectionTestUtils} — basta instanciar a classe
 * com um limite baixo para tornar o teste rápido e determinístico.
 */
@ExtendWith(MockitoExtension.class)
class GlobalRateLimitFilterTest {

    private static final int LIMITE = 3;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private StringWriter corpoDaResposta;

    private GlobalRateLimitFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new GlobalRateLimitFilter(LIMITE);

        corpoDaResposta = new StringWriter();
        PrintWriter writer = new PrintWriter(corpoDaResposta);
        lenient().when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void requisicoesDentroDoLimiteDevemPassarPeloFiltroSemAlterarStatus() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        for (int i = 0; i < LIMITE; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(LIMITE)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void requisicaoAlemDoLimiteDeveSerBloqueadaCom429EMensagemDeErro() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        for (int i = 0; i < LIMITE; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        // Requisição extra, além do limite configurado.
        filter.doFilterInternal(request, response, filterChain);

        // O filterChain só deve ter sido chamado para as LIMITE requisições que passaram.
        verify(filterChain, times(LIMITE)).doFilter(request, response);
        verify(response, times(1)).setStatus(429);
        verify(response, times(1)).setContentType("application/json");
        assertThat(corpoDaResposta.toString())
                .isEqualTo("{\"erro\": \"Muitas requisições. Tente novamente em instantes.\"}");
    }

    @Test
    void requisicaoBloqueadaNaoDeveChamarOFilterChain() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        for (int i = 0; i < LIMITE; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        corpoDaResposta.getBuffer().setLength(0);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(LIMITE)).doFilter(request, response);
    }

    @Test
    void ipsDiferentesDevemTerContadoresIndependentes() throws Exception {
        when(request.getRemoteAddr()).thenReturn("192.168.0.1", "192.168.0.1", "192.168.0.1", "10.0.0.5");

        // Esgota o limite para o primeiro IP (3 requisições).
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Requisição de um IP diferente: deve passar normalmente, sem sofrer o bloqueio do primeiro IP.
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(4)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void mesmoIpAposEsgotarLimiteDeveContinuarBloqueadoEmChamadasSubsequentes() throws Exception {
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        for (int i = 0; i < LIMITE; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        // Duas requisições extras, ambas devem ser bloqueadas dentro da mesma janela.
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(LIMITE)).doFilter(request, response);
        verify(response, times(2)).setStatus(429);
    }
}
