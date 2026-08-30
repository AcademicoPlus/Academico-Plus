package com.unifapi.academico.config;

import com.unifapi.academico.service.CodigoVerificacaoMessage;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * O monolito só publica na exchange abaixo — fila, binding e DLQ são declaradas
 * e mantidas pelo academico-email-service, que é o consumidor. Declarar a mesma
 * exchange aqui (idempotente) evita erro de "exchange not found" caso o monolito
 * suba antes do email-service.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String ROUTING_KEY_CODIGO_VERIFICACAO = "codigo-verificacao";

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE, true, false);
    }

    /**
     * Id lógico "codigoVerificacao" é o contrato combinado com o academico-email-service
     * (que não compartilha esta classe — é um módulo Maven independente).
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setIdClassMapping(Map.of("codigoVerificacao", CodigoVerificacaoMessage.class));
        converter.setClassMapper(classMapper);
        return converter;
    }
}
