package com.linkedout.auth.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 마이크로서비스의 응답을 담는 모델 클래스
 * <p>
 * 이 클래스는 마이크로서비스가 요청을 처리한 후 생성하는 응답 데이터를 표현합니다.
 * RabbitMQ를 통해 마이크로서비스에서 API Gateway로 전송되며,
 * API Gateway는 이 응답 데이터를 다시 HTTP 응답으로 변환하여 클라이언트에게 전달합니다.
 * <p>
 * {@code @Data}:
 * - Lombok 애노테이션으로 게터, 세터, equals(), hashCode(), toString() 메서드를 자동 생성
 * - 코드를 간결하게 유지할 수 있게 해줌
 * <p>
 * Serializable 인터페이스 구현:
 * - RabbitMQ를 통해 객체를 전송하기 위해 직렬화 가능해야 함
 * - 객체를 바이트 스트림으로 변환하여 네트워크로 전송하거나 파일에 저장할 수 있게 함
 */
@Data
public class ResponseData implements Serializable {
	/**
	 * 응답과 요청을 연결하는 상관관계 ID
	 * <p>
	 * 이 ID는 요청 시 생성되어 RabbitMQ 메시지 속성에 포함되며,
	 * 마이크로서비스는 응답 시 이 ID를 그대로 반환합니다.
	 * API Gateway는 이 ID를 통해 어떤 요청에 대한 응답인지 식별합니다.
	 */
	private String correlationId;

	/**
	 * HTTP 상태 코드
	 * <p>
	 * 마이크로서비스가 설정한 응답 상태 코드입니다.
	 * 예: 200(OK), 404(Not Found), 500(Internal Server Error) 등
	 * API Gateway는 이 코드를 HTTP 응답의 상태 코드로 사용합니다.
	 */
	private int statusCode;

	/**
	 * HTTP 응답 헤더
	 * <p>
	 * 키-값 쌍으로 구성된 응답 헤더 정보를 담습니다.
	 * 마이크로서비스가 설정한 헤더들이 포함됩니다.
	 * 예: Content-Type: application/json
	 */
	private Map<String, String> headers;

	/**
	 * 응답 본문
	 * <p>
	 * 마이크로서비스가 생성한 응답 내용을 문자열로 저장합니다.
	 * 일반적으로 JSON 형식의 데이터가 포함되지만, 다른 형식(XML, HTML 등)일 수도 있습니다.
	 * API Gateway는 이 내용을 HTTP 응답 본문으로 사용합니다.
	 */
	private String body;
}