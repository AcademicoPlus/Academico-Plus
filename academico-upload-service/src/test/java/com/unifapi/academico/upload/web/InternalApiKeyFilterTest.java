package com.unifapi.academico.upload.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InternalApiKeyFilterTest {

    private final InternalApiKeyFilter filter = new InternalApiKeyFilter();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain filterChain = mock(FilterChain.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "internalApiKey", "chave-secreta");
    }

    @Test
    void devePermitirRequisicaoComChaveCorreta() throws Exception {
        when(request.getRequestURI()).thenReturn("/fotos");
        when(request.getHeader("X-Internal-Api-Key")).thenReturn("chave-secreta");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void deveBloquearRequisicaoComChaveIncorreta() throws Exception {
        when(request.getRequestURI()).thenReturn("/fotos");
        when(request.getHeader("X-Internal-Api-Key")).thenReturn("chave-errada");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void devePermitirActuatorSemChave() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
