/**
 * AdminUserReportResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminUserReportResponse {

    // 신고 ID
    private Long id;
    // 신고한 유저 ID
    private Long reporterUserId;
    // 신고 대상 유저 ID
    private Long reportedUserId;
    // 신고 유형 코드
    private String reasonCode;
    // 신고 상세
    private String detail;
    // 처리 상태
    private String status;
    // 운영자 메모
    private String adminNote;
    // 신고 시각
    private LocalDateTime createdAt;
    private String resolutionAction;
    private Integer suspensionDays;
    private java.time.Instant resolvedAt;
    private String publicReason;
}
