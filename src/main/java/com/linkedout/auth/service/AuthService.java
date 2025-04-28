package com.linkedout.auth.service;

import com.linkedout.auth.dto.AuthRequest;
import com.linkedout.auth.dto.AuthResponse;
import com.linkedout.auth.dto.RequestData;
import com.linkedout.auth.dto.ResponseData;
import com.linkedout.auth.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final ClientRegistrationRepository clientRegistrationRepository;
	private final JwtService jwtService;
	private final RestTemplate restTemplate;

	public void processOAuthRequest(AuthRequest request, AuthResponse response) {
		String provider = request.getProvider();
		String code = request.getCode();

		if (provider == null || code == null) {
			response.setSuccess(false);
			response.setError("Provider and code must be provided");
			return;
		}

		try {
			ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(provider);
			if (registration == null) {
				response.setSuccess(false);
				response.setError("Unknown provider: " + provider);
				return;
			}

			// OAuth 토큰 교환 수행
			Map<String, String> tokenRequest = new HashMap<>();
			tokenRequest.put("code", code);
			tokenRequest.put("client_id", registration.getClientId());
			tokenRequest.put("client_secret", registration.getClientSecret());
			tokenRequest.put("redirect_uri", request.getRedirectUri());
			tokenRequest.put("grant_type", "authorization_code");

			// 토큰 요청 보내기
			String tokenUri = registration.getProviderDetails().getTokenUri();
			Map<String, Object> tokenResponse = restTemplate.postForObject(
				tokenUri, tokenRequest, Map.class);

			if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
				response.setSuccess(false);
				response.setError("Failed to obtain access token");
				return;
			}

			String accessToken = (String) tokenResponse.get("access_token");

			// 사용자 정보 가져오기
			String userInfoUri = registration.getProviderDetails().getUserInfoEndpoint().getUri();
			Map<String, Object> userAttributes = restTemplate.getForObject(
				userInfoUri + "?access_token=" + accessToken, Map.class);

			if (userAttributes == null) {
				response.setSuccess(false);
				response.setError("Failed to fetch user info");
				return;
			}

			// 사용자 객체 생성
			User user = User.builder()
				.id(UUID.randomUUID().toString())
				.email((String) userAttributes.get("email"))
				.name((String) userAttributes.get("name"))
				.picture((String) userAttributes.get("picture"))
				.roles(Collections.singleton("ROLE_USER"))
				.provider(provider)
				.build();

			// JWT 토큰 생성
			String token = jwtService.generateToken(user);

			response.setSuccess(true);
			response.setToken(token);
			response.setUser(user);

		} catch (Exception e) {
			log.error("OAuth authentication error", e);
			response.setSuccess(false);
			response.setError("OAuth authentication error: " + e.getMessage());
		}
	}

	public void processTokenValidation(AuthRequest request, AuthResponse response) {
		String token = request.getToken();

		if (token == null) {
			response.setSuccess(false);
			response.setError("Token must be provided");
			return;
		}

		try {
			// 토큰에서 정보 추출
			String userId = jwtService.extractUsername(token);
			String email = jwtService.extractClaim(token, claims -> claims.get("email", String.class));
			String name = jwtService.extractClaim(token, claims -> claims.get("name", String.class));

			// 토큰 만료 확인
			if (jwtService.isTokenExpired(token)) {
				response.setSuccess(false);
				response.setError("Token has expired");
				return;
			}

			User user = User.builder()
				.id(userId)
				.email(email)
				.name(name)
				.build();

			response.setSuccess(true);
			response.setUser(user);

		} catch (Exception e) {
			log.error("Token validation error", e);
			response.setSuccess(false);
			response.setError("Token validation error: " + e.getMessage());
		}
	}

	public void processLogout(AuthRequest request, AuthResponse response) {
		// 로그아웃 처리는 클라이언트 측에서 토큰을 삭제하는 방식으로 구현
		// 서버 측에서는 특별한 처리가 필요 없음
		response.setSuccess(true);
	}

	public void test(RequestData request, ResponseData response) {
		response.setStatusCode(200); // OK
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");

		String responseBody = "{\"success\":true,\"message\":\"Successfully logged out\"}";
		response.setBody(responseBody);
	}
}