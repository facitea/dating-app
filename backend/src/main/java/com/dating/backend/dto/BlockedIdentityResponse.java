/**
 * BlockedIdentityResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BlockedIdentityResponse {

    // 차단 ID
    private Long id;
    // 차단 유형
    private String identityType;
    // 차단 값
    private String identityValue;
    // 차단 근거 유저 ID
    private Long sourceUserId;
    // 차단 근거 이메일
    private String sourceUserEmail;
    // 차단 근거 닉네임
    private String sourceUserNickname;
    // 차단 사유
    private String reason;
    // 활성 여부
    private Boolean active;
    // 등록 시각
    private LocalDateTime createdAt;
    // 변경 시각
    private LocalDateTime updatedAt;
}
