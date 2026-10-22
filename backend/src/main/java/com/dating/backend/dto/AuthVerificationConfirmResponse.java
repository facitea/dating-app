/**
 * 자체 인증번호 확인 응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthVerificationConfirmResponse {

    // 사용자 안내 메시지
    private String message;

    // 인증 완료 후 회원가입 요청에 사용할 토큰
    private String verificationToken;
}
