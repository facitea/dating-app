/**
 * ScamMonitorUserResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScamMonitorUserResponse {

    // 유저 ID
    private Long userId;
    // 이메일
    private String email;
    // 닉네임
    private String nickname;
    // 가입 경로
    private String provider;
    // 계정 상태
    private String status;
    // 스캠 위험 점수
    private Integer fraudRiskScore;
    // 스캠 검토 상태
    private String fraudReviewStatus;
    // 운영자 메모
    private String adminMemo;
    // 차단 정보 존재 여부
    private boolean identityBlocked;
    // 차단된 신원 유형
    private String blockedIdentityTypes;
    // 거주 지역
    private String region;
    // 직업
    private String job;
    // 미처리 신고 건수
    private long openReportCount;
    // 최근 위험 사유
    private String latestRiskDetail;
    // 가입일
    private LocalDateTime createdAt;
}
