package com.linkedout.auth.exception;

/**
 * 서버 내부 오류가 발생했을 때 사용되는 예외를 나타냅니다.
 * <p>
 * 이 예외는 {@code BaseException}을 확장하여 HTTP 500 Internal Server Error
 * 상태 코드와 연관되며, 서버가 요청을 처리하는 동안 예상치 못한 상황이 감지되었음을 나타냅니다.
 * <p>
 * 이 예외는 애플리케이션 내에서 특정 서버 오류를 포착하고 관련 정보를 제공하며,
 * 효율적인 오류 처리와 디버깅을 가능하게 합니다.
 */
public class InternalServerErrorException extends BaseException {
	/**
	 * 서버 내부 오류가 발생했을 때 사용되는 예외입니다.
	 * 이 예외는 HTTP 500 Internal Server Error 상태 코드와 함께,
	 * 서버가 요청을 처리하는 도중 예기치 못한 상황이 발생했음을 나타냅니다.
	 *
	 * @param message 예외와 관련된 사용자 정의 오류 메시지
	 */
	public InternalServerErrorException(String message) {
		super(message, 500);
	}
}