/**
 * PhoneVerificationRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhoneVerificationRequest {

    @NotBlank(message = "휴대폰 번호를 입력해 주세요.")
    @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호는 01012345678 형식으로 입력해 주세요.")
    // 휴대폰 번호
    private String phone;
}
