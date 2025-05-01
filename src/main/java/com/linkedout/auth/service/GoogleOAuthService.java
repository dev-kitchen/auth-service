package com.linkedout.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.linkedout.auth.utils.JwtUtil;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthRequest;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthResponse;
import com.linkedout.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

	private final JwtUtil jwtUtil;

	// 안드로이드용 클라이언트 ID 추가
	@Value("${google.oauth2.android.client-id}")
	private String androidClientId;

	/**
	 * 안드로이드 앱에서 받은 구글 OAuth 코드를 처리
	 * 안드로이드 클라이언트 ID 사용
	 */
	public GoogleOAuthResponse processAndroidOAuthCode(GoogleOAuthRequest request) {
		try {
			// 1. 구글 OAuth 코드를 이용해 토큰 요청
			// (안드로이드 클라이언트는 secret이 없으므로 serverAuthCode 방식 사용)
			GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
				new NetHttpTransport(),
				GsonFactory.getDefaultInstance(),
				"https://oauth2.googleapis.com/token",  // 고정된 토큰 엔드포인트
				androidClientId,
				"",  // 안드로이드 클라이언트는 시크릿이 없음
				request.getCode(),
				request.getRedirectUri())
				.execute();

			// 2. ID 토큰 검증
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
				new NetHttpTransport(), GsonFactory.getDefaultInstance())
				.setAudience(Collections.singletonList(androidClientId))
				.build();

			GoogleIdToken idToken = verifier.verify(tokenResponse.getIdToken());
			if (idToken == null) {
				throw new IllegalArgumentException("유효하지 않은 ID 토큰입니다.");
			}

			// 3. 사용자 정보 추출
			Payload payload = idToken.getPayload();
			String email = payload.getEmail();

			// 이미 존재하는 유저인지 확인, 없으면 생성해야함

			// 4. 자체 JWT 토큰 발급
			// todo 사용자 아이디 추가
			// todo 타입 추가
			Map<String, Object> claims = new HashMap<>();
			claims.put("email", email);
			claims.put("name", payload.get("name"));


			String accessToken = jwtUtil.generateToken(claims, email);
			String refreshToken = jwtUtil.generateRefreshToken(email);

			// 5. 응답 맵 생성
			// todo 타입 추가

			return GoogleOAuthResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.email(email)
				.name((String) payload.get("name"))
				.picture((String) payload.get("picture"))
				.build();

		} catch (IOException | GeneralSecurityException e) {
			log.error("Google OAuth 처리 중 오류 발생", e);
			throw new BadRequestException("Google OAuth 처리 중 오류가 발생했습니다: " + e.getMessage());
		}
	}
}
