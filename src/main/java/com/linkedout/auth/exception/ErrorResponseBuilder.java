package com.linkedout.auth.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.dto.ResponseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorResponseBuilder {
	private final ObjectMapper objectMapper;

	/**
	 * HTTP 응답용 에러 정보 생성 (Spring Controller에서 사용)
	 */
	public ResponseEntity<Object> buildHttpErrorResponse(Exception ex, HttpStatus status) {
		String message = ex.getMessage();

		Map<String, Object> body = createErrorBody(status.value(), message);
		return new ResponseEntity<>(body, status);
	}

	/**
	 * ResponseData 형식의 에러 정보 생성 (RabbitMQ 응답용)
	 */
	public ResponseData buildMqErrorResponse(int statusCode, String message) {

		ResponseData response = new ResponseData();
		response.setStatusCode(statusCode);
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");

		try {
			response.setBody(objectMapper.writeValueAsString(createErrorBody(statusCode, message)));
		} catch (JsonProcessingException e) {
			response.setBody("{\"success\":false,\"message\":\"" + message + "\"}");
		}

		return response;
	}

	/**
	 * 기존 ResponseData 객체에 에러 정보 설정 (RabbitMQ 응답용)
	 */
	public void populateErrorResponse(ResponseData response, int statusCode, String message) {

		response.setStatusCode(statusCode);
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");

		try {
			response.setBody(objectMapper.writeValueAsString(createErrorBody(statusCode, message)));
		} catch (JsonProcessingException e) {
			response.setBody("{\"success\":false,\"message\":\"" + message + "\"}");
		}
	}

	/**
	 * 공통 에러 응답 본문 생성
	 */
	private Map<String, Object> createErrorBody(int statusCode, String message) {
		Map<String, Object> errorBody = new HashMap<>();
		errorBody.put("success", false);
		errorBody.put("status", statusCode);
		errorBody.put("message", message);
		return errorBody;
	}
}
