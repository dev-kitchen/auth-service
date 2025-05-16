package com.linkedout.auth.api;

import com.linkedout.auth.service.GoogleOAuthService;
import com.linkedout.common.model.dto.auth.AuthResponseDTO;
import com.linkedout.common.model.dto.auth.oauth.google.GoogleOAuthResponseDTO;
import com.linkedout.common.model.dto.auth.oauth.google.GoogleUserInfoDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
public class AuthController {
	private final GoogleOAuthService googleOAuthService;

	@GetMapping("/google")
	public void googleLogin(HttpServletResponse response) throws IOException {
		String authUrl = googleOAuthService.getGoogleAuthUrl();
		response.sendRedirect(authUrl);
	}

	@GetMapping("/google/callback")
	public void googleCallback(@RequestParam String code, HttpServletResponse response)
		throws IOException {
		// 코드로 토큰 교환
		GoogleOAuthResponseDTO tokenResponse = googleOAuthService.getGoogleToken(code);

		// 토큰으로 사용자 정보 가져오기
		GoogleUserInfoDTO userInfo = googleOAuthService.getGoogleUserInfo(tokenResponse.getAccessToken());

		// 사용자 정보로 로그인 또는 회원가입 처리
		AuthResponseDTO authResponse = googleOAuthService.loginOrSignup(userInfo);

		String redirectUrl =
			"devKitchen://oauthredirect"
				+ "?access_token="
				+ authResponse.getAccessToken()
				+ "&refresh_token="
				+ authResponse.getRefreshToken();

		log.info("Google OAuth redirect: {}", redirectUrl);
		response.sendRedirect(redirectUrl);
	}
}
