/**
 * 예전 요청 본문 방식 refresh token DTO입니다.
 * 현재 refresh token은 HttpOnly Cookie로 전달하므로 새 코드에서는 사용하지 않습니다.
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    // 로그인 유지용 refresh token
    @NotBlank(message = "refresh token을 입력해 주세요.")
    private String refreshToken;
}
