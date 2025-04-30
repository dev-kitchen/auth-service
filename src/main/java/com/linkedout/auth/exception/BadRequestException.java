package com.linkedout.auth.exception;


/**
 * 클라이언트가 잘못되거나 형식에 맞지 않는 요청을 보낼 때 발생하는 예외를 나타냅니다.
 * 이 예외는 HTTP 400 Bad Request 상태 코드와 연관되어 있습니다.
 * <p>
 * 이 예외는 {@code BaseException}을 확장하여 구문이나 내용 문제로 인해
 * 클라이언트 요청을 정상적으로 처리할 수 없는 상황에서
 * 사용자 정의 오류 메시지를 제공할 수 있도록 합니다.
 */
public class BadRequestException extends BaseException {
	/**
	 * 잘못되거나 형식에 맞지 않는 클라이언트 요청으로 인해 발생하는 예외입니다(400)
	 *
	 * @param message 예외와 관련된 사용자 정의 오류 메시지
	 */
	public BadRequestException(String message) {
		super(message, 400);
	}
}
