package com.unifapi.academico.service;

import com.unifapi.academico.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodigoVerificacaoPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Enfileira o código de verificação para o {@code academico-email-service}.
     *
     * <p>Nunca propaga exceção. Falha do consumidor já não voltava para o
     * chamador (a entrega é assíncrona), mas <strong>broker indisponível</strong>
     * voltava: {@code convertAndSend} lança {@link AmqpException}, que subia até
     * o controller e virava 500 — ou seja, uma queda do CloudAMQP derrubava a
     * recuperação de senha com um erro feio em vez de uma mensagem tratada.
     *
     * <p>Além disso, {@code solicitarResetSenha} é anotado com
     * {@code @Transactional}: a exceção fazia rollback do OTP recém-gravado,
     * então nem o registro da tentativa sobrava para diagnóstico.
     *
     * <p>Engolir e logar (em vez de avisar o usuário que o envio falhou) é
     * deliberado: o endpoint responde sempre a mesma mensagem genérica para não
     * virar um oráculo de enumeração de contas. Avisar da falha só no caso em
     * que a conta existe reintroduziria exatamente esse vazamento. O preço é que
     * uma queda do broker só aparece no log e na DLQ — ver a pendência de
     * observabilidade no README.
     *
     * @return {@code true} se foi enfileirado, {@code false} se o broker falhou.
     */
    public boolean publicar(String destinatario, String codigo, int minutosExpiracao) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EMAIL_EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_CODIGO_VERIFICACAO,
                    new CodigoVerificacaoMessage(destinatario, codigo, minutosExpiracao));
            return true;
        } catch (AmqpException e) {
            log.error("Falha ao enfileirar código de verificação — o e-mail NÃO será enviado. "
                    + "Verifique o broker (RabbitMQ/CloudAMQP).", e);
            return false;
        }
    }
}
