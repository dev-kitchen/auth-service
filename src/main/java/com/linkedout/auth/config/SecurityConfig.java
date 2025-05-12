package com.linkedout.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/oauth2/test").permitAll()  // 테스트 경로 허용
				.requestMatchers("/api/auth/oauth2/google").permitAll()  // 구글 로그인 시작 경로 허용
				.requestMatchers("/api/auth/oauth2/google/callback").permitAll()  // 콜백 경로 허용
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth2 -> oauth2
				.loginPage("/api/auth/oauth2/google")  // 로그인 페이지 경로 지정
				.defaultSuccessUrl("/api/auth/oauth2/google/callback")  // 성공 시 리다이렉션 경로
			);

		return http.build();
	}
}
