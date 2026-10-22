/**
 * AdminReviewCandidateResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class AdminReviewCandidateResponse {

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
    // 심사 코멘트
    private String reviewComment;
    // 운영자 메모
    private String adminMemo;
    // 스캠 위험 점수
    private Integer fraudRiskScore;
    // 스캠 검토 상태
    private String fraudReviewStatus;
    // 가입일
    private LocalDateTime createdAt;
    // 심사 마감일
    private LocalDateTime reviewDeadlineAt;
    // 프로필 완료 여부
    private boolean profileComplete;
    // 남은 심사일수
    private long remainingDays;
    // 생년월일
    private LocalDate birthDate;
    // 성별
    private String gender;
    // 거주 지역
    private String region;
    // 직업
    private String job;
// MBTI
    private String mbti;
    // 성격
    private String personality;
    // 이상형
    private String idealType;
    // 자기소개
    private String introduction;
    // 프로필 사진 목록
    private List<UserProfileImageResponse> images;
}
