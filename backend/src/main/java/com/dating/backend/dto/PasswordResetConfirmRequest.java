/**
 * PasswordResetConfirmRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetConfirmRequest {

    @NotBlank
    // 재설정 토큰
    private String token;

    @NotBlank
    @Size(min = 8, max = 100)
    // 새 비밀번호
    private String newPassword;
}
