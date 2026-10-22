/**
 * 자체 인증번호 발급 요청 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthVerificationRequest {

    // 인증 대상 종류
    @NotBlank(message = "인증 대상 종류를 입력해 주세요.")
    @Pattern(regexp = "EMAIL|PHONE", message = "인증 대상은 EMAIL 또는 PHONE만 사용할 수 있습니다.")
    private String targetType;

    // 인증 대상 값
    @NotBlank(message = "인증 대상을 입력해 주세요.")
    private String targetValue;

    // 인증 목적
    @NotBlank(message = "인증 목적을 입력해 주세요.")
    @Pattern(regexp = "SIGNUP|LOGIN|PASSWORD_RESET", message = "지원하지 않는 인증 목적입니다.")
    private String purpose;
}
