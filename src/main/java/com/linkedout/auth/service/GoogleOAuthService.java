package com.linkedout.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.auth.utils.JwtUtil;
import com.linkedout.common.messaging.ServiceMessageClient;
import com.linkedout.common.model.dto.account.AccountDTO;
import com.linkedout.common.model.dto.auth.AuthResponseDTO;
import com.linkedout.common.model.dto.auth.oauth.google.GoogleOAuthResponseDTO;
import com.linkedout.common.model.dto.auth.oauth.google.GoogleUserInfoDTO;
import com.linkedout.common.util.MonoPipe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
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
	private final ObjectMapper objectMapper;
	private final ServiceMessageClient messageClient;
	private final WebClient webClient;

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
	public Mono<GoogleOAuthResponseDTO> getGoogleToken(String code) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("code", code);
		formData.add("client_id", clientId);
		formData.add("client_secret", clientSecret);
		formData.add("redirect_uri", redirectUri);
		formData.add("grant_type", "authorization_code");

		return MonoPipe.ofMono(webClient.post()
				.uri("https://oauth2.googleapis.com/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(formData)
				.retrieve()
				.bodyToMono(String.class))
			.then(responseBody -> {
				try {
					return objectMapper.readValue(responseBody, GoogleOAuthResponseDTO.class);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("토큰 응답 파싱 오류", e);
				}
			})
			.handleError(e -> log.error("구글 토큰 요청 중 오류: {}", e.getMessage(), e))
			.result();
	}

	/**
	 * 액세스 토큰으로 구글 사용자 정보 요청
	 */
	public Mono<GoogleUserInfoDTO> getGoogleUserInfo(String accessToken) {
		return MonoPipe.ofMono(webClient.get()
				.uri("https://www.googleapis.com/oauth2/v3/userinfo")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.retrieve()
				.bodyToMono(String.class))
			.then(responseBody -> {
				try {
					return objectMapper.readValue(responseBody, GoogleUserInfoDTO.class);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("사용자 정보 파싱 오류", e);
				}
			})
			.handleError(e -> log.error("구글 사용자 정보 요청 중 오류: {}", e.getMessage(), e))
			.result();
	}

	/**
	 * 사용자 정보로 로그인 또는 회원가입 처리
	 */
	public Mono<AuthResponseDTO> loginOrSignup(GoogleUserInfoDTO userInfo) {
		// 이메일로 사용자 조회 및 없으면 생성
		return MonoPipe.ofMono(findOrCreateAccount(userInfo))
			.thenFlatMap(this::generateAuthResponse)
			.handleError(e -> log.error("로그인/회원가입 중 오류: {}", e.getMessage(), e))
			.result();
	}

	/**
	 * 계정 조회 또는 생성 (추출한 헬퍼 메서드)
	 */
	private Mono<AccountDTO> findOrCreateAccount(GoogleUserInfoDTO userInfo) {
		return MonoPipe.ofMono(messageClient.sendMessage("account", "getFindByEmail", userInfo.getEmail(), AccountDTO.class))
			.then(existingAccount -> existingAccount)
			.handleEmpty(() -> {
				log.info("계정이 존재하지 않음. 새로 생성합니다.");
				return messageClient.sendMessage("account", "postCreateAccount", userInfo, AccountDTO.class);
			})
			.result();
	}

	/**
	 * JWT 발급 및 응답 생성 (추출한 헬퍼 메서드)
	 */
	private Mono<AuthResponseDTO> generateAuthResponse(AccountDTO account) {
		// JWT 발급
		Map<String, Object> claims = new HashMap<>();
		claims.put("accountId", account.getId());
		claims.put("email", account.getEmail());
		claims.put("name", account.getName());

		List<String> roles = Collections.singletonList(String.valueOf(account.getRole()));
		claims.put("roles", roles);

		String accessToken = jwtUtil.generateToken(claims, account.getId());
		String refreshToken = jwtUtil.generateRefreshToken(account.getId());

		return MonoPipe.of(AuthResponseDTO.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.email(account.getEmail())
				.name(account.getName())
				.profileImage(account.getPicture())
				.build())
			.result();
	}
}
