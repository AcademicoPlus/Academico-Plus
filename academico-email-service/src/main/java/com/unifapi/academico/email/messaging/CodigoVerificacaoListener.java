package com.unifapi.academico.email.messaging;

import com.unifapi.academico.email.config.RabbitMQConfig;
import com.unifapi.academico.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodigoVerificacaoListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CODIGO_VERIFICACAO)
    public void receber(CodigoVerificacaoMessage mensagem) {
        boolean enviado = emailService.enviarCodigoVerificacao(
                mensagem.destinatario(), mensagem.codigo(), mensagem.minutosExpiracao());

        if (!enviado) {
            // Sem retry aqui: a fila está configurada com default-requeue-rejected=false,
            // então lançar joga a mensagem direto pra DLQ em vez de reprocessar em loop.
            throw new IllegalStateException("Falha ao enviar e-mail para " + mensagem.destinatario());
        }
        log.info("Código de verificação enviado para {}", mensagem.destinatario());
    }
}
