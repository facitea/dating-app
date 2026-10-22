/**
 * AuthResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {

    // access token
    private String token;
    // refresh token
    private String refreshToken;
    // 사용자 정보
    private UserResponse user;
}
