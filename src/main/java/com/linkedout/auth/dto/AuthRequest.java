package com.linkedout.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    private String type; // "oauth", "token-validate", "logout"
    private String provider; // "google", "facebook", etc.
    private String code; // OAuth authorization code
    private String redirectUri; // 리다이렉트 URI
    private String state; // OAuth state
    private String token; // JWT token for validation
    private String correlationId; // 요청과 응답을 매칭하기 위한 ID
}
