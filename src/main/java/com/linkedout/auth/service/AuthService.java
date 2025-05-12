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
				response.setStatusCode(200);
				response.setHeaders(new HashMap<>());
				response.getHeaders().put("Content-Type", "application/json");
				response.setBody(result);
			})
			.doOnError(error -> {
				errorResponseBuilder.populateErrorResponse(response, 500, "서비스 통신 오류");
			})
			.then();
	}
}