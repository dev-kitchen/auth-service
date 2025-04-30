package com.linkedout.auth.api;

import com.linkedout.auth.dto.AuthRequest;
import com.linkedout.auth.dto.AuthResponse;
import com.linkedout.common.constant.RabbitMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final RabbitTemplate rabbitTemplate;
	private final ConcurrentHashMap<String, CompletableFuture<AuthResponse>> pendingResponses = new ConcurrentHashMap<>();

	/**
	 * OAuth2 콜백 처리를 위한 웹훅
	 * API Gateway가 OAuth 리다이렉션 후 이 엔드포인트를 호출
	 */
	@GetMapping("/oauth2/callback/{provider}")
	public ResponseEntity<?> oauthCallback(
		@PathVariable String provider,
		@RequestParam String code,
		@RequestParam(required = false) String state,
		@RequestParam(required = false) String redirectUri) {

		log.info("OAuth callback received for provider: {}", provider);
		log.info("Code: {}, State: {}, RedirectUri: {}", code, state, redirectUri);

		String correlationId = UUID.randomUUID().toString();

		AuthRequest request = AuthRequest.builder()
			.type("oauth")
			.provider(provider)
			.code(code)
			.state(state)
			.redirectUri(redirectUri)
			.correlationId(correlationId)
			.build();

		try {
			// 응답을 기다리기 위한 CompletableFuture 생성
			CompletableFuture<AuthResponse> futureResponse = new CompletableFuture<>();
			pendingResponses.put(correlationId, futureResponse);

			// RabbitMQ로 메시지 전송
			rabbitTemplate.convertAndSend(
				RabbitMQConstants.AUTH_EXCHANGE,
				RabbitMQConstants.AUTH_ROUTING_KEY,
				request
			);

			// 응답 대기 (최대 10초)
			AuthResponse response = futureResponse.get(10, TimeUnit.SECONDS);
			pendingResponses.remove(correlationId);

			if (response.isSuccess()) {
				// 성공 응답 반환
				return ResponseEntity.ok(response);
			} else {
				// 실패 응답 반환
				return ResponseEntity.badRequest().body(response);
			}

		} catch (Exception e) {
			log.error("Error processing OAuth callback", e);
			pendingResponses.remove(correlationId);
			return ResponseEntity.internalServerError().body("Error processing OAuth callback: " + e.getMessage());
		}
	}

	/**
	 * 토큰 검증을 위한 엔드포인트
	 */
	@PostMapping("/validate")
	public ResponseEntity<?> validateToken(@RequestBody AuthRequest request) {
		String correlationId = UUID.randomUUID().toString();
		request.setType("token-validate");
		request.setCorrelationId(correlationId);

		try {
			CompletableFuture<AuthResponse> futureResponse = new CompletableFuture<>();
			pendingResponses.put(correlationId, futureResponse);

			rabbitTemplate.convertAndSend(
				RabbitMQConstants.AUTH_EXCHANGE,
				RabbitMQConstants.AUTH_ROUTING_KEY,
				request
			);

			AuthResponse response = futureResponse.get(5, TimeUnit.SECONDS);
			pendingResponses.remove(correlationId);

			if (response.isSuccess()) {
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.badRequest().body(response);
			}

		} catch (Exception e) {
			log.error("Error validating token", e);
			pendingResponses.remove(correlationId);
			return ResponseEntity.internalServerError().body("Error validating token: " + e.getMessage());
		}
	}

	/**
	 * 로그아웃을 위한 엔드포인트
	 */
	@PostMapping("/logout")
	public ResponseEntity<?> logout(@RequestBody AuthRequest request) {
		String correlationId = UUID.randomUUID().toString();
		request.setType("logout");
		request.setCorrelationId(correlationId);

		try {
			CompletableFuture<AuthResponse> futureResponse = new CompletableFuture<>();
			pendingResponses.put(correlationId, futureResponse);

			rabbitTemplate.convertAndSend(
				RabbitMQConstants.AUTH_EXCHANGE,
				RabbitMQConstants.AUTH_ROUTING_KEY,
				request
			);

			AuthResponse response = futureResponse.get(5, TimeUnit.SECONDS);
			pendingResponses.remove(correlationId);

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("Error processing logout", e);
			pendingResponses.remove(correlationId);
			return ResponseEntity.internalServerError().body("Error processing logout: " + e.getMessage());
		}
	}
}
