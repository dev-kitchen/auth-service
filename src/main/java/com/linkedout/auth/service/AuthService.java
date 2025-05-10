package com.linkedout.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.dto.ApiRequestData;
import com.linkedout.common.dto.ApiResponseData;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthRequest;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthResponse;
import com.linkedout.common.exception.BadRequestException;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.exception.InternalServerErrorException;
import com.linkedout.common.messaging.ServiceMessageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final ObjectMapper objectMapper;
	private final ServiceMessageClient messageClient;
	private final GoogleOAuthService googleOAuthService;
	private final ErrorResponseBuilder errorResponseBuilder;

	public Mono<Void> health(ApiRequestData request, ApiResponseData response) {
		response.setStatusCode(200); // OK
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");

		String responseBody = "{\"success\":true,\"message\":\"I'm alive\"}";
		response.setBody(responseBody);
		return Mono.empty();
	}


	public Mono<Void> test(ApiRequestData request, ApiResponseData response) {
		TestDto testRequest = new TestDto();
		testRequest.setMessage("테스트 메시지");
		return messageClient.sendMessage("account", "test", testRequest, String.class)
			.doOnNext(result -> {
				response.setStatusCode(202);
				response.setHeaders(new HashMap<>());
				response.getHeaders().put("Content-Type", "application/json");
				response.setBody(result);
			})
			.doOnError(error -> {
				errorResponseBuilder.populateErrorResponse(response, 500, "서비스 통신 오류");
			})
			.then();
	}


	/**
	 * 안드로이드용 구글 OAuth 요청을 처리하는 메서드
	 * 클라이언트에서 받은 인증 코드를 안드로이드 클라이언트 ID로 검증하고 AT/RT 발급
	 */
	public void processOAuthRequest(ApiRequestData request, ApiResponseData response) {
		GoogleOAuthRequest oauthRequest;
		try {
			oauthRequest = objectMapper.readValue(request.getBody(), GoogleOAuthRequest.class);
		} catch (JsonProcessingException e) {
			// 예외 처리기가 처리하도록 throw
			throw new BadRequestException("잘못된 요청 형식입니다: " + e.getMessage());
		}

		// 안드로이드용 구글 OAuth 코드 처리 및 토큰 발급
		GoogleOAuthResponse result = googleOAuthService.processAndroidOAuthCode(oauthRequest.getIdToken());

		// 응답 생성
		response.setStatusCode(200);
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");
		try {
			response.setBody(objectMapper.writeValueAsString(result));
		} catch (JsonProcessingException e) {
			throw new InternalServerErrorException("응답 생성 중 오류 발생: " + e.getMessage());
		}
	}
}