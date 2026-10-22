/**
 * AdminPhoneVerificationLogResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import com.dating.backend.entity.PhoneVerification;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminPhoneVerificationLogResponse {

    // 인증 로그 ID
    private Long id;
    // 휴대폰 번호(마스킹)
    private String phone;
    // 인증 코드
    private String code;
    // 인증 성공 여부
    private Boolean verified;
    // 인증 완료 시각
    private LocalDateTime verifiedAt;
    // 시도 횟수
    private Integer attemptCount;
    // 만료 시각
    private LocalDateTime expiresAt;
    // 요청 생성 시각
    private LocalDateTime createdAt;

    public static AdminPhoneVerificationLogResponse from(PhoneVerification verification) {
        return new AdminPhoneVerificationLogResponse(
                verification.getId(),
                maskPhone(verification.getPhone()),
                verification.getCode(),
                verification.getVerified(),
                verification.getVerifiedAt(),
                verification.getAttemptCount(),
                verification.getExpiresAt(),
                verification.getCreatedAt()
        );
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
