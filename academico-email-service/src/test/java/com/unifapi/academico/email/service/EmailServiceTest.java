package com.unifapi.academico.email.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private final TemplateEngine templateEngine = mock(TemplateEngine.class);
    private final EmailService emailService = new EmailService(templateEngine);

    @Test
    void deveRetornarSucessoEmModoSimulacaoQuandoChaveDoResendNaoEstaConfigurada() {
        when(templateEngine.process(org.mockito.ArgumentMatchers.eq("email/otp-verificacao"),
                org.mockito.ArgumentMatchers.any())).thenReturn("<html></html>");
        ReflectionTestUtils.setField(emailService, "resendApiKey", "");

        boolean enviado = emailService.enviarCodigoVerificacao("usuario@teste.com", "123456", 15);

        assertThat(enviado).isTrue();
    }

    @Test
    void deveRetornarFalsoQuandoChaveDoResendEstaConfiguradaMasChamadaFalha() {
        when(templateEngine.process(org.mockito.ArgumentMatchers.eq("email/otp-verificacao"),
                org.mockito.ArgumentMatchers.any())).thenReturn("<html></html>");
        // Chave com prefixo válido, mas api-url aponta pra um host inexistente -> falha de conexão.
        ReflectionTestUtils.setField(emailService, "resendApiKey", "re_chave_de_teste");
        ReflectionTestUtils.setField(emailService, "resendApiUrl", "http://host-invalido.invalid/emails");

        boolean enviado = emailService.enviarCodigoVerificacao("usuario@teste.com", "123456", 15);

        assertThat(enviado).isFalse();
    }
}
