/**
 * User JPA 엔티티
 */
package com.dating.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private String sanctionType;
    private java.time.Instant suspendedUntil;
    private String statusBeforeSanction;
    @Column(length=1000) private String sanctionReason;
    @Column(nullable=false,columnDefinition="integer default 0") private int warningCount;
    @Column(nullable=false,columnDefinition="bigint default 0") private long totalBlockCount;

    private boolean resting;
    private long sessionVersion;
    @Builder.Default private String profileReviewStatus="NONE";
    private long profileRevision;
    @Column(length=1000) private String profileReviewNote;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 유저 ID
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    // 이메일
    private String email;

    @Column(name = "nickname", nullable = false, unique = true, length = 255)
    // 닉네임
    private String nickname;

    @Column(name = "phone", length = 20)
    // 휴대폰 번호
    private String phone;

    @Column(name = "provider", nullable = false, length = 20)
    // 가입 경로
    private String provider;

    @Column(name = "status", nullable = false, length = 30)
    // 계정 상태
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "review_comment", length = 255)
    // 심사 코멘트
    private String reviewComment;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    // 운영자 메모
    private String adminMemo;

    @Column(name = "fraud_risk_score", nullable = false)
    // 스캠 위험 점수
    @Builder.Default
    private Integer fraudRiskScore = 0;

    @Column(name = "fraud_review_status", nullable = false, length = 20)
    // 스캠 검토 상태
    @Builder.Default
    private String fraudReviewStatus = "NORMAL";

    @Column(name = "password_hash", length = 255)
    // 비밀번호 해시
    private String password;

    @Column(name = "reset_password_token", length = 255)
    // 비밀번호 재설정 토큰
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expires_at")
    // 재설정 토큰 만료 시각
    private LocalDateTime resetPasswordTokenExpiresAt;

    @Column(name = "review_deadline_at")
    // 심사 마감일
    private LocalDateTime reviewDeadlineAt;

    @Column(name = "last_warning_sent_at")
    // 마지막 경고 발송 시각
    private LocalDateTime lastWarningSentAt;

    @Column(name = "profile_completed_at")
    // 프로필 완료 시각
    private LocalDateTime profileCompletedAt;

    @Column(name = "deleted_at")
    // 삭제 시각
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    // 생성 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    // 수정 시각
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
