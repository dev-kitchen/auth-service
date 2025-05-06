package com.linkedout.auth.api.consumer;

import com.linkedout.auth.service.AuthService;
import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.dto.ApiRequestData;
import com.linkedout.common.dto.ApiResponseData;
import com.linkedout.common.dto.ServiceRequestData;
import com.linkedout.common.exception.BaseException;
import com.linkedout.common.exception.ErrorResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceConsumer {
	private final RabbitTemplate rabbitTemplate;
	private final AuthService authService;
	private final ErrorResponseBuilder errorResponseBuilder;


	@RabbitListener(queues = RabbitMQConstants.AUTH_SERVICE_QUEUE)
	public void processServiceRequest(ServiceRequestData<?> request, @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
		log.info("받은 요청: {}, correlationId: {}", request, correlationId);


		ApiResponseData response = new ApiResponseData();
		response.setCorrelationId(correlationId);
		response.setHeaders(new HashMap<>());

		// 요청 처리를 Mono로 래핑
		Mono.fromCallable(() -> {
				try {
					switch (request.getRequestKey()) {
//						case "test" -> authService.test(request, response);
						default ->
							errorResponseBuilder.populateErrorResponse(response, 404, "요청을 처리할 수 없습니다: " + request.getRequestKey());
					}
				} catch (BaseException ex) {
					errorResponseBuilder.populateErrorResponse(response, ex.getStatusCode(), ex.getMessage());
				}
				return response;
			})
			.subscribeOn(Schedulers.boundedElastic())
			.subscribe(completedResponse -> {
				// 응답 전송
				rabbitTemplate.convertAndSend(
//					RabbitMQConstants.AUTH_EXCHANGE,
//					RabbitMQConstants.AUTH_RESPONSE_ROUTING_KEY,
					completedResponse
				);
				log.info("응답 전송: {}", completedResponse);
			});
	}
}
