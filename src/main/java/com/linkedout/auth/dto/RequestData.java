package com.linkedout.auth.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 마이크로서비스로부터 받은 응답 데이터를 담는 모델 클래스
 * <p>
 * 이 클래스는 RabbitMQ를 통해 마이크로서비스가 API Gateway로 보내는 응답을 표현합니다.
 * 마이크로서비스는 요청을 처리한 후, 그 결과를 이 클래스의 인스턴스로 만들어
 * 'gateway-response-queue'에 전송합니다.
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
public class RequestData implements Serializable {
	/**
	 * 요청 경로
	 * 예: /api/users/123
	 */
	private String path;

	/**
	 * HTTP 메서드
	 * 예: GET, POST, PUT, DELETE 등
	 */
	private String method;

	/**
	 * HTTP 요청 헤더
	 * 키-값 쌍으로 구성된 헤더 정보를 담음
	 * 예: Content-Type: application/json
	 */
	private Map<String, String> headers;

	/**
	 * 요청 본문
	 * HTTP 요청의 body 내용을 문자열로 저장
	 * JSON, XML, 폼 데이터 등이 될 수 있음
	 */
	private String body;

	/**
	 * URL 쿼리 파라미터
	 * 키-값 쌍으로 구성된 쿼리 파라미터 정보를 담음
	 * 예: ?name=John&age=30
	 */
	private Map<String, String> queryParams;
}