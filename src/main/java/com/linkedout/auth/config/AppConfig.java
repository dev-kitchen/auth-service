package com.linkedout.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.messaging.ResponseRegistry;
import com.linkedout.common.messaging.ServiceMessageClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public ErrorResponseBuilder errorResponseBuilder(ObjectMapper objectMapper) {
		return new ErrorResponseBuilder(objectMapper);
	}

	@Bean
	public ServiceMessageClient serviceMessageClient(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
		return new ServiceMessageClient(rabbitTemplate, objectMapper);
	}

}
