package com.unifapi.academico.service;

import com.unifapi.academico.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * O ponto destes testes é a garantia de que o publisher nunca propaga exceção:
 * {@code solicitarResetSenha} responde sempre a mesma mensagem genérica, e um
 * broker fora do ar não pode transformar isso num 500 (nem, por rollback da
 * transação, apagar o OTP recém-gravado).
 */
@ExtendWith(MockitoExtension.class)
class CodigoVerificacaoPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CodigoVerificacaoPublisher publisher;

    @Test
    void devePublicarNaExchangeERoutingKeyDoCodigoDeVerificacao() {
        boolean enfileirou = publisher.publicar("alguem@unifapi.edu.br", "123456", 15);

        assertThat(enfileirou).isTrue();
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EMAIL_EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY_CODIGO_VERIFICACAO),
                eq(new CodigoVerificacaoMessage("alguem@unifapi.edu.br", "123456", 15)));
    }

    @Test
    void naoDevePropagarExcecaoQuandoOBrokerEstaForaDoAr() {
        doThrow(new AmqpConnectException(new RuntimeException("conexão recusada")))
                .when(rabbitTemplate)
                .convertAndSend(anyExchange(), anyRoutingKey(), any(CodigoVerificacaoMessage.class));

        assertThatCode(() -> publisher.publicar("alguem@unifapi.edu.br", "123456", 15))
                .doesNotThrowAnyException();
    }

    @Test
    void deveSinalizarFalhaNoRetornoQuandoOBrokerEstaForaDoAr() {
        doThrow(new AmqpConnectException(new RuntimeException("conexão recusada")))
                .when(rabbitTemplate)
                .convertAndSend(anyExchange(), anyRoutingKey(), any(CodigoVerificacaoMessage.class));

        assertThat(publisher.publicar("alguem@unifapi.edu.br", "123456", 15)).isFalse();
    }

    // convertAndSend(String, String, Object) tem sobrecargas demais para o
    // any() cru resolver sozinho; estes dois deixam a escolha explícita.
    private static String anyExchange() {
        return eq(RabbitMQConfig.EMAIL_EXCHANGE);
    }

    private static String anyRoutingKey() {
        return eq(RabbitMQConfig.ROUTING_KEY_CODIGO_VERIFICACAO);
    }
}
