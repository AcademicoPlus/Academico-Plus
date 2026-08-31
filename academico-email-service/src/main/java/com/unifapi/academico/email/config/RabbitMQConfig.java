package com.unifapi.academico.email.config;

import com.unifapi.academico.email.messaging.CodigoVerificacaoMessage;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Este serviço é o dono da topologia da fila de e-mail: o monolito só conhece
 * o nome da exchange e a routing key, e publica sem declarar fila nenhuma.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "email.exchange";
    public static final String ROUTING_KEY_CODIGO_VERIFICACAO = "codigo-verificacao";
    public static final String QUEUE_CODIGO_VERIFICACAO = "email.codigo-verificacao";
    public static final String QUEUE_CODIGO_VERIFICACAO_DLQ = "email.codigo-verificacao.dlq";

    private static final String DEAD_LETTER_EXCHANGE = "email.dlx";

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue codigoVerificacaoQueue() {
        return QueueBuilder.durable(QUEUE_CODIGO_VERIFICACAO)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE_CODIGO_VERIFICACAO_DLQ)
                .build();
    }

    @Bean
    public Queue codigoVerificacaoDlq() {
        return QueueBuilder.durable(QUEUE_CODIGO_VERIFICACAO_DLQ).build();
    }

    @Bean
    public Binding codigoVerificacaoBinding(Queue codigoVerificacaoQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(codigoVerificacaoQueue).to(emailExchange).with(ROUTING_KEY_CODIGO_VERIFICACAO);
    }

    @Bean
    public Binding codigoVerificacaoDlqBinding(Queue codigoVerificacaoDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(codigoVerificacaoDlq).to(deadLetterExchange).with(QUEUE_CODIGO_VERIFICACAO_DLQ);
    }

    /**
     * O monolito e este serviço não compartilham as classes de mensagem (são módulos
     * Maven independentes), então o {@code __TypeId__} do Jackson não pode ser a classe
     * concreta (FQN diferente dos dois lados). O id lógico "codigoVerificacao" é o
     * contrato combinado entre os dois — precisa bater com o id configurado no
     * publisher do monolito.
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
