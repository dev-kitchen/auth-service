package com.linkedout.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.auth.utils.JwtUtil;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.messaging.ServiceMessageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final ObjectMapper objectMapper;
	private final ServiceMessageClient messageClient;
	private final GoogleOAuthService googleOAuthService;
	private final ErrorResponseBuilder errorResponseBuilder;
	private final JwtUtil jwtUtil;


	public Mono<String> getTestToken() {
		Map<String, Object> claims = new HashMap<>();
		claims.put("accountId", 4);
		claims.put("email", "daechan476@gmail.com");
		claims.put("name", "DaeChan Jo");

		List<String> roles = Collections.singletonList("ROLE_USER");
		claims.put("roles", roles);

		String accessToken = jwtUtil.generateToken(claims, 4L);
		String refreshToken = jwtUtil.generateRefreshToken(4L);

		return Mono.just(accessToken);
	}
}
