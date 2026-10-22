/**
 * 자체 인증번호 발급 응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuthVerificationStartResponse {

    // 사용자 안내 메시지
    private String message;

    // 인증번호 만료 시각
    private LocalDateTime expiresAt;

    // 개발 환경에서 확인할 인증번호
    private String devCode;
}
