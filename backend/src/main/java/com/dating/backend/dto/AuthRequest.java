/**
 * AuthRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {

    @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
    @NotBlank(message = "이메일을 입력해 주세요.")
    // 이메일
    private String email;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상 입력해 주세요.")
    // 비밀번호
    private String password;
}
