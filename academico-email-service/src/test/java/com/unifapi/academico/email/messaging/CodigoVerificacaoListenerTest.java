package com.unifapi.academico.email.messaging;

import com.unifapi.academico.email.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodigoVerificacaoListenerTest {

    @Mock
    private EmailService emailService;

    private CodigoVerificacaoListener listener;

    @Test
    void deveDelegarParaEmailServiceComOsDadosDaMensagem() {
        listener = new CodigoVerificacaoListener(emailService);
        CodigoVerificacaoMessage mensagem = new CodigoVerificacaoMessage("usuario@teste.com", "123456", 15);
        when(emailService.enviarCodigoVerificacao("usuario@teste.com", "123456", 15)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> listener.receber(mensagem));

        verify(emailService).enviarCodigoVerificacao("usuario@teste.com", "123456", 15);
    }

    @Test
    void deveLancarExcecaoQuandoEnvioFalhaParaAcionarDlq() {
        listener = new CodigoVerificacaoListener(emailService);
        CodigoVerificacaoMessage mensagem = new CodigoVerificacaoMessage("usuario@teste.com", "123456", 15);
        when(emailService.enviarCodigoVerificacao("usuario@teste.com", "123456", 15)).thenReturn(false);

        assertThatThrownBy(() -> listener.receber(mensagem))
                .isInstanceOf(IllegalStateException.class);
    }
}
