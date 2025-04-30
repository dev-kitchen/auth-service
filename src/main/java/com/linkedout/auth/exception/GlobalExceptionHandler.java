package com.linkedout.auth.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.linkedout.common.dto.ResponseData;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final ObjectMapper objectMapper;

	@ExceptionHandler(BaseException.class)
	public ResponseData handleBaseException(BaseException ex) {
		log.error("BaseException 발생: {}", ex.getMessage());
		return createErrorResponse(ex.getStatusCode(), ex.getMessage());
	}

	// JSON 파싱 예외 처리
	@ExceptionHandler(JsonProcessingException.class)
	public ResponseData handleJsonProcessingException(JsonProcessingException ex) {
		log.error("JSON 파싱 오류: {}", ex.getMessage());
		return createErrorResponse(400, "잘못된 요청 형식입니다: " + ex.getMessage());
	}

	// IllegalArgumentException 처리
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseData handleIllegalArgumentException(IllegalArgumentException ex) {
		log.error("유효성 오류: {}", ex.getMessage());
		return createErrorResponse(400, ex.getMessage());
	}

	// 기타 모든 예외 처리
	@ExceptionHandler(Exception.class)
	public ResponseData handleException(Exception ex) {
		log.error("서버 내부 오류: {}", ex.getMessage(), ex);
		return createErrorResponse(500, "서버 내부 오류가 발생했습니다: " + ex.getMessage());
	}

	// ResponseData 형식으로 에러 응답 생성 (기존 응답 구조 유지)
	private ResponseData createErrorResponse(int statusCode, String message) {
		ResponseData response = new ResponseData();
		response.setStatusCode(statusCode);
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");

		try {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", statusCode);
			errorResponse.put("success", false);
			errorResponse.put("message", message);
			response.setBody(objectMapper.writeValueAsString(errorResponse));
		} catch (JsonProcessingException e) {
			// 매우 드문 케이스
			response.setBody("{\"success\":false,\"message\":\"" + message + "\"}");
		}

		return response;
	}
}