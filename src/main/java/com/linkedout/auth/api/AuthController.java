package com.linkedout.auth.api;

import com.linkedout.auth.service.GoogleOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
public class AuthController {
	private final GoogleOAuthService googleOAuthService;

	@GetMapping("/google")
	public Mono<Void> googleLogin(ServerHttpResponse response) {
		String authUrl = googleOAuthService.getGoogleAuthUrl();
		response.getHeaders().setLocation(URI.create(authUrl));
		response.setStatusCode(HttpStatus.FOUND); // 302 리다이렉트
		return response.setComplete();
	}

	@GetMapping("/google/callback")
	public Mono<Void> googleCallback(@RequestParam String code, ServerHttpResponse response) {
		return googleOAuthService.getGoogleToken(code)
			.flatMap(tokenResponse -> googleOAuthService.getGoogleUserInfo(tokenResponse.getAccessToken())
				.flatMap(userInfo -> googleOAuthService.loginOrSignup(userInfo)
					.map(authResponse -> {
						String redirectUrl = "devKitchen://oauthredirect"
							+ "?access_token=" + authResponse.getAccessToken()
							+ "&refresh_token=" + authResponse.getRefreshToken();

						log.info("Google OAuth redirect: {}", redirectUrl);
						return redirectUrl;
					})
					.flatMap(redirectUrl -> {
						response.getHeaders().setLocation(URI.create(redirectUrl));
						response.setStatusCode(HttpStatus.FOUND);
						return response.setComplete();
					})
				)
			);
	}
}
