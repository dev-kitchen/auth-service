package com.linkedout.auth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "auth-exchange";
    public static final String AUTH_QUEUE = "auth-queue";
    public static final String GATEWAY_QUEUE = "api-gateway-queue";
    public static final String AUTH_ROUTING_KEY = "auth.request";
    public static final String AUTH_RESPONSE_ROUTING_KEY = "auth.response";

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue authQueue() {
        return new Queue(AUTH_QUEUE, false);
    }

    @Bean
    public Queue authResponseQueue() {
        return new Queue(GATEWAY_QUEUE, false);
    }

    @Bean
    public Binding authBinding(Queue authQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(authQueue).to(authExchange).with(AUTH_ROUTING_KEY);
    }

    @Bean
    public Binding authResponseBinding(Queue authResponseQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(authResponseQueue).to(authExchange).with(AUTH_RESPONSE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
