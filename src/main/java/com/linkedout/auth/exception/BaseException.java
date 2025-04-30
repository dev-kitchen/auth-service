package com.linkedout.auth.exception;

import lombok.Getter;

/**
 * HTTP 상태 코드와 연관된 사용자 정의 런타임 예외의 기본 클래스입니다.
 * <p>
 * 이 클래스는 사용자 정의 메시지와 HTTP 상태 코드를 모두 포함하는
 * 특정 예외를 생성하기 위한 기반이 됩니다. 애플리케이션의 다양한 오류 시나리오를
 * 나타내기 위해 확장될 수 있으며, 구조화된 오류 정보를 제공합니다.
 */
@Getter
public class BaseException extends RuntimeException {
	private final int statusCode;

	/**
	 * HTTP 상태 코드와 메시지를 기반으로 사용자 정의 런타임 예외를 생성합니다.
	 *
	 * @param message    예외와 관련된 사용자 정의 오류 메시지
	 * @param statusCode 예외와 연관된 HTTP 상태 코드
	 */
	public BaseException(String message, int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}
}

