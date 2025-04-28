package com.linkedout.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RabbitMQ 구성 테스트 클래스
 * 임포트가 제대로 되는지 확인하는 용도
 */
public class RabbitMQConfigTest {

    @Test
    public void importCheckTest() {
        // 심볼들이 제대로 임포트되는지 확인하는 테스트
        // 컴파일만 되면 성공
        System.out.println("RabbitMQ 관련 심볼들이 제대로 임포트됩니다.");
    }
}
