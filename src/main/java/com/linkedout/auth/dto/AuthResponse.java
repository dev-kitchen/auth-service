package com.linkedout.auth.dto;

import com.linkedout.auth.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private boolean success;
    private String token;
    private User user;
    private String error;
    private String correlationId; // 요청과 응답을 매칭하기 위한 ID
}
