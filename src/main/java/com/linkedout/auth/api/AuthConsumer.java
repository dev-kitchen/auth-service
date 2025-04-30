package com.linkedout.auth.api;

import com.linkedout.auth.exception.BaseException;
import com.linkedout.auth.exception.ErrorResponseBuilder;
import com.linkedout.auth.service.AuthService;
import com.linkedout.common.constant.RabbitMQConstants;
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
public class AuthConsumer {

	private final RabbitTemplate rabbitTemplate;
	private final AuthService authService;
	private final ErrorResponseBuilder errorResponseBuilder;

	@RabbitListener(queues = RabbitMQConstants.AUTH_QUEUE)
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
				case "GET /api/auth/error" -> authService.error(request, response);
				case "POST /api/auth/google/android" -> authService.processOAuthRequest(request, response);
				default -> errorResponseBuilder.populateErrorResponse(response, 404, "요청을 처리할 수 없습니다: " + requestKey);
			}
		} catch (BaseException ex) {
			errorResponseBuilder.populateErrorResponse(response, ex.getStatusCode(), ex.getMessage());
		}

		rabbitTemplate.convertAndSend(
			RabbitMQConstants.AUTH_EXCHANGE,
			RabbitMQConstants.AUTH_RESPONSE_ROUTING_KEY,
			response
		);

		log.info("응답 전송: {}", response);
	}
}