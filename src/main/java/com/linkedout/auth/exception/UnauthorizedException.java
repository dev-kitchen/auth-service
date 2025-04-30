package com.linkedout.auth.exception;

/**
 * 인증되지 않은 접근 시도가 감지될 때 발생하는 예외를 나타냅니다.
 * <p>
 * 이 예외는 {@code BaseException}의 특수한 하위 클래스이며
 * HTTP 401 Unauthorized 상태 코드와 연관되어 있습니다. 일반적으로 클라이언트가
 * 요청된 리소스에 접근하기 위해 자신을 인증해야 함을 나타내는 데 사용됩니다.
 */
public class UnauthorizedException extends BaseException {
	/**
	 * 인증되지 않은 접근 시도가 감지될 때 발생하는 예외를 생성합니다.
	 * 이 예외는 HTTP 401 Unauthorized 상태 코드와 함께 발생하며,
	 * 클라이언트가 요청된 리소스에 접근하려면 인증이 필요함을 나타냅니다.
	 *
	 * @param message 예외와 관련된 사용자 정의 오류 메시지
	 */
	public UnauthorizedException(String message) {
		super(message, 401);
	}
}