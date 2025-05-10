package com.linkedout.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.linkedout.auth.repository.AccountRepository;
import com.linkedout.auth.utils.JwtUtil;
import com.linkedout.common.dto.auth.AuthResponse;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthResponse;
import com.linkedout.common.dto.auth.oauth.google.GoogleUserInfo;
import com.linkedout.common.entity.Account;
import com.linkedout.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final AccountRepository accountRepository;

	// 안드로이드용 클라이언트 ID 추가
	@Value("${google.oauth2.android.client-id}")
	private String androidClientId;

	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String clientId;

	@Value("${spring.security.oauth2.client.registration.google.client-secret}")
	private String clientSecret;

	@Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
	private String redirectUri;

	/**
	 * 구글 인증 URL 생성
	 */
	public String getGoogleAuthUrl() {
		return "https://accounts.google.com/o/oauth2/auth" +
			"?client_id=" + clientId +
			"&redirect_uri=" + redirectUri +
			"&response_type=code" +
			"&scope=email%20profile";
	}

	/**
	 * 구글 인증 코드로 액세스 토큰 요청
	 */
	public GoogleOAuthResponse getGoogleToken(String code) {
		String tokenUrl = "https://oauth2.googleapis.com/token";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("code", code);
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("redirect_uri", redirectUri);
		body.add("grant_type", "authorization_code");

		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, requestEntity, String.class);

		try {
			return objectMapper.readValue(response.getBody(), GoogleOAuthResponse.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("토큰 응답 파싱 오류", e);
		}
	}

	/**
	 * 액세스 토큰으로 구글 사용자 정보 요청
	 */
	public GoogleUserInfo getGoogleUserInfo(String accessToken) {
		String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(
			userInfoUrl, HttpMethod.GET, requestEntity, String.class);

		try {
			return objectMapper.readValue(response.getBody(), GoogleUserInfo.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("사용자 정보 파싱 오류", e);
		}
	}


	/**
	 * 사용자 정보로 로그인 또는 회원가입 처리
	 */
	public AuthResponse loginOrSignup(GoogleUserInfo userInfo) {
		// 이메일로 사용자 조회
		// todo Account 분리
//		Account acount = serviceMessageClient.sendMessage(RabbitMQConstants.ACCOUNT_SERVICE_QUEUE, request, AccountDTO.class).flatMqp()
		Account account = accountRepository.findByEmail(userInfo.getEmail())
			.orElseGet(() -> createUser(userInfo));  // 없으면 새로 생성


		// JWT 토큰 발급
		// todo 사용자 아이디 추가
		Map<String, Object> claims = new HashMap<>();
		claims.put("email", account.getEmail());
		claims.put("name", account.getName());
		claims.put("accountId", account.getId());

		String accessToken = jwtUtil.generateToken(claims, account.getEmail());
		String refreshToken = jwtUtil.generateRefreshToken(account.getEmail());


		return AuthResponse.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.email(account.getEmail())
			.name(account.getName())
			.profileImage(account.getPicture())
			.build();
	}

	/**
	 * 신규 사용자 생성
	 */
	// todo Account 분리
	private Account createUser(GoogleUserInfo userInfo) {
		return Account.builder()
			.email(userInfo.getEmail())
			.name(userInfo.getName())
			.picture(userInfo.getPicture())
			.provider("google")
			.providerId(userInfo.getSub())
			.build();
	}

	/**
	 * 안드로이드 앱에서 받은 구글 OAuth 코드를 처리
	 * 안드로이드 클라이언트 ID 사용
	 */
	public GoogleOAuthResponse processAndroidOAuthCode(String idTokenString) {
		try {
			// 1. ID 토큰 검증
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
				new NetHttpTransport(), GsonFactory.getDefaultInstance())
				.setAudience(Collections.singletonList(androidClientId))
				.build();

			GoogleIdToken idToken = verifier.verify(idTokenString);
			if (idToken == null) {
				throw new IllegalArgumentException("유효하지 않은 ID 토큰입니다.");
			}

			// 2. 사용자 정보 추출
			Payload payload = idToken.getPayload();
			String email = payload.getEmail();
			String name = (String) payload.get("name");
			String picture = (String) payload.get("picture");

			// 이미 존재하는 계정인지 확인, 없으면 생성해야함

			// 4. 자체 JWT 토큰 발급
			// todo 사용자 아이디 추가
			Map<String, Object> claims = new HashMap<>();
			claims.put("email", email);
			claims.put("name", name);
//			claims.put("accountId", account.getId());


			String accessToken = jwtUtil.generateToken(claims, email);
			String refreshToken = jwtUtil.generateRefreshToken(email);


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
