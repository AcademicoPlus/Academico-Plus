package com.unifapi.academico.service;

import com.unifapi.academico.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodigoVerificacaoPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicar(String destinatario, String codigo, int minutosExpiracao) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_CODIGO_VERIFICACAO,
                new CodigoVerificacaoMessage(destinatario, codigo, minutosExpiracao));
    }
}
