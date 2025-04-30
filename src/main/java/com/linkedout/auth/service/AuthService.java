package com.linkedout.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.auth.dto.GoogleOAuthRequest;
import com.linkedout.auth.exception.BadRequestException;
import com.linkedout.auth.exception.InternalServerErrorException;
import com.linkedout.auth.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.linkedout.common.dto.RequestData;
import com.linkedout.common.dto.ResponseData;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final ObjectMapper objectMapper;
	private final GoogleOAuthService googleOAuthService;

	public void health(RequestData request, ResponseData response) {
		response.setStatusCode(201); // OK
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");

		String responseBody = "{\"success\":true,\"message\":\"I'm alive\"}";
		response.setBody(responseBody);
	}

	public void error(RequestData request, ResponseData response) {
		throw new UnauthorizedException("에러테스트");
	}

	/**
	 * 안드로이드용 구글 OAuth 요청을 처리하는 메서드
	 * 클라이언트에서 받은 인증 코드를 안드로이드 클라이언트 ID로 검증하고 AT/RT 발급
	 */
	public void processOAuthRequest(RequestData request, ResponseData response) {
		GoogleOAuthRequest oauthRequest;
		try {
			oauthRequest = objectMapper.readValue(request.getBody(), GoogleOAuthRequest.class);
		} catch (JsonProcessingException e) {
			// 예외 처리기가 처리하도록 throw
			throw new BadRequestException("잘못된 요청 형식입니다: " + e.getMessage());
		}

		// 안드로이드용 구글 OAuth 코드 처리 및 토큰 발급
		Map<String, String> result = googleOAuthService.processAndroidOAuthCode(oauthRequest);

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