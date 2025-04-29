package com.linkedout.auth.controller;

import com.linkedout.auth.config.RabbitMQConfig;
import com.linkedout.auth.service.AuthService;
import com.rabbitmq.client.MessageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Controller;
import com.linkedout.common.dto.RequestData;
import com.linkedout.common.dto.ResponseData;

import java.util.HashMap;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthMessageListener {

	private final RabbitTemplate rabbitTemplate;
	private final AuthService authService;

	@RabbitListener(queues = RabbitMQConfig.AUTH_QUEUE)
	public void processAuthRequest(RequestData request, @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
		log.info("받은 요청: {}, correlationId: {}", request, correlationId);


		ResponseData response = new ResponseData();
		response.setCorrelationId(correlationId);
		response.setHeaders(new HashMap<>());

		try {
			String path = request.getPath();
			String method = request.getMethod();
			String requestKey = method + " " + path;

			switch (requestKey) {
				case "GET /api/auth/health" -> authService.health(request, response);
//				case "POST /api/auth/oauth" -> authService.processOAuthRequest(request, response);
//				case "GET /api/auth/validate" -> authService.processTokenValidation(request, response);
//				case "POST /api/auth/logout" -> authService.processLogout(request, response);
				default -> {
					response.setStatusCode(404);
					response.getHeaders().put("Content-Type", "application/json");
					response.setBody("{\"error\":\"Unknown request: " + requestKey + "\"}");
				}
			}
		} catch (Exception e) {
			log.error("요청 처리중 에러 발생", e);
			// 500 Internal Server Error 응답 생성
			response.setStatusCode(500);
			response.getHeaders().put("Content-Type", "application/json");
			response.setBody("{\"error\":\"" + e.getMessage() + "\"}");
		}

		rabbitTemplate.convertAndSend(
			RabbitMQConfig.EXCHANGE_NAME,
			RabbitMQConfig.AUTH_RESPONSE_ROUTING_KEY,
			response
		);

		log.info("응답 전송: {}", response);
	}
}