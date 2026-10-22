/**
 * UserReport JPA 엔티티
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
@Table(name = "user_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReport {
    private String resolutionAction;
    private Integer suspensionDays;
    private java.time.Instant resolvedAt;
    @Column(length=1000) private String publicReason;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 신고 ID
    private Long id;

    @Column(name = "reporter_user_id", nullable = false)
    // 신고한 유저 ID
    private Long reporterUserId;

    @Column(name = "reported_user_id", nullable = false)
    // 신고 대상 유저 ID
    private Long reportedUserId;

    @Column(name = "reason_code", nullable = false, length = 30)
    // 신고 사유 코드
    private String reasonCode;

    @Column(name = "detail", nullable = false, columnDefinition = "TEXT")
    // 신고 상세
    private String detail;

    @Column(name = "status", nullable = false, length = 20)
    // 처리 상태
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "admin_note", columnDefinition = "TEXT")
    // 운영자 메모
    private String adminNote;

    @Column(name = "created_at", nullable = false)
    // 신고 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
