package com.linkedout.auth.api.consumer;

import com.linkedout.auth.service.AuthService;
import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.dto.ApiRequestData;
import com.linkedout.common.dto.ApiResponseData;
import com.linkedout.common.dto.ServiceMessageDTO;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.exception.BaseException;
import com.linkedout.common.messaging.ServiceIdentifier;
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
public class AuthConsumer {

  private final RabbitTemplate rabbitTemplate;
  private final AuthService authService;
  private final ErrorResponseBuilder errorResponseBuilder;
  private final ServiceIdentifier serviceIdentifier;

  @RabbitListener(queues = RabbitMQConstants.AUTH_API_QUEUE)
  public void processApiRequest(
      ApiRequestData request, @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
    log.info("받은 요청: {}, correlationId: {}", request, correlationId);

    ApiResponseData response = new ApiResponseData();
    response.setCorrelationId(correlationId);
    response.setHeaders(new HashMap<>());

    Mono.defer(
            () -> {
              String path = request.getPath();
              String method = request.getMethod();
              String requestKey = method + " " + path;

              try {
                return switch (requestKey) {
                  case "GET /api/auth/test" ->
                      authService.test(request, response).thenReturn(response);
                  case "GET /api/auth/health" ->
                      authService.health(request, response).thenReturn(response);
                  default -> {
                    errorResponseBuilder.populateErrorResponse(
                        response, 404, "지원하지 않는 작업: " + requestKey);
                    yield Mono.just(response);
                  }
                };
              } catch (BaseException ex) {
                errorResponseBuilder.populateErrorResponse(
                    response, ex.getStatusCode(), ex.getMessage());
                return Mono.just(response);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            completedResponse -> {
              // 응답 전송
              rabbitTemplate.convertAndSend(
                  RabbitMQConstants.API_EXCHANGE,
                  RabbitMQConstants.API_GATEWAY_ROUTING_KEY,
                  completedResponse);
              log.info("응답 전송: {}", completedResponse);
            });
  }

  //	/**
  //	 * 다른 서비스로부터의 메시지 요청 처리
  //	 * 이 큐는 auth 서비스가 다른 서비스로부터 메시지를 받는 큐
  //	 */
  //	@RabbitListener(queues = RabbitMQConstants.AUTH_SERVICE_CONSUMER_QUEUE)
  //	public ServiceMessageDTO<?> processServiceRequest(ServiceMessageDTO<?> requestMessage) {
  //		String correlationId = requestMessage.getCorrelationId();
  //		String operation = requestMessage.getOperation();
  //
  //		log.info("서비스 요청 수신: correlationId={}, operation={}, sender={}",
  //			correlationId, operation, requestMessage.getSenderService());
  //
  //		try {
  //			// 작업 타입에 따른 처리 분기
  //			Object result = switch (operation) {
  //				case "getAccountByEmail" -> handleGetAccountByEmail(requestMessage);
  //				case "createAccount" -> handleCreateAccount(requestMessage);
  //				case "validateToken" -> handleValidateToken(requestMessage);
  //				case "refreshToken" -> handleRefreshToken(requestMessage);
  //				default -> throw new UnsupportedOperationException("지원하지 않는 작업: " + operation);
  //			};
  //
  //			// 응답 메시지 생성
  //			return ServiceMessageDTO.builder()
  //				.correlationId(correlationId)
  //				.senderService(serviceIdentifier.getServiceName())
  //				.operation(operation + "Response")
  //				.payload(result)
  //				.build();
  //		} catch (Exception e) {
  //			log.error("서비스 요청 처리 오류: {}", e.getMessage(), e);
  //
  //			// 오류 응답 생성
  //			return ServiceMessageDTO.builder()
  //				.correlationId(correlationId)
  //				.senderService(serviceIdentifier.getServiceName())
  //				.operation(operation + "Response")
  //				.error(e.getMessage())
  //				.build();
  //		}
  //	}
}
