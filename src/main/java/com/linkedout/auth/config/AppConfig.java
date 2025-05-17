package com.linkedout.auth.config;

import com.linkedout.common.messaging.ServiceIdentifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {
	@Value("${service.name}")
	private String serviceName;

	@Bean
	public ServiceIdentifier serviceIdentifier() {
		return new ServiceIdentifier(serviceName);
	}

	@Bean
	public WebClient webClient() {
		return WebClient.builder()
			.codecs(configurer -> configurer
				.defaultCodecs()
				.maxInMemorySize(16 * 1024 * 1024)) // 버퍼 크기 설정 (옵션)
			.build();
	}
}

