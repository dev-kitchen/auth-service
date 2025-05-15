package com.linkedout.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.auth.utils.JwtUtil;
import com.linkedout.common.messaging.ServiceMessageClient;
import com.linkedout.common.model.dto.account.AccountDTO;
import com.linkedout.common.model.dto.auth.AuthResponse;
import com.linkedout.common.model.dto.auth.oauth.google.GoogleOAuthResponse;
import com.linkedout.common.model.dto.auth.oauth.google.GoogleUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

	private final JwtUtil jwtUtil;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final ServiceMessageClient messageClient;

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
		return "https://accounts.google.com/o/oauth2/auth"
			+ "?client_id="
			+ clientId
			+ "&redirect_uri="
			+ redirectUri
			+ "&response_type=code"
			+ "&scope=email%20profile";
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

		ResponseEntity<String> response =
			restTemplate.postForEntity(tokenUrl, requestEntity, String.class);

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

		ResponseEntity<String> response =
			restTemplate.exchange(userInfoUrl, HttpMethod.GET, requestEntity, String.class);

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
		AccountDTO account =
			messageClient
				.sendMessage("account", "getFindByEmail", userInfo.getEmail(), AccountDTO.class)
				.doOnNext(existingAccount -> log.info("계정 조회 결과: {}", existingAccount))
				.switchIfEmpty(
					Mono.defer(
						() -> {
							log.info("계정이 존재하지 않음. 새로 생성합니다.");
							return messageClient.sendMessage(
								"account", "postCreateAccount", userInfo, AccountDTO.class);
						}))
				.onErrorMap(e -> new RuntimeException("계정 조회/생성 중 오류 발생", e))
				.block();

		if (account == null) {
			throw new RuntimeException("계정을 생성할 수 없습니다.");
		}


		// todo 등록된 조리기구..?
		// todo 핑거프린트
		// JWT 발급
		Map<String, Object> claims = new HashMap<>();
		claims.put("accountId", account.getId());
		claims.put("email", account.getEmail());
		claims.put("name", account.getName());

		List<String> roles = Collections.singletonList(String.valueOf(account.getRole()));
		claims.put("roles", roles);

		String accessToken = jwtUtil.generateToken(claims, account.getId());
		String refreshToken = jwtUtil.generateRefreshToken(account.getId());

		return AuthResponse.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.email(account.getEmail())
			.name(account.getName())
			.profileImage(account.getPicture())
			.build();
	}
}
