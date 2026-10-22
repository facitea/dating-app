/**
 * FraudRiskLog JPA 엔티티
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
@Table(name = "fraud_risk_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRiskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 로그 ID
    private Long id;

    @Column(name = "user_id", nullable = false)
    // 유저 ID
    private Long userId;

    @Column(name = "risk_type", nullable = false, length = 30)
    // 위험 유형
    private String riskType;

    @Column(name = "score", nullable = false)
    // 점수
    private Integer score;

    @Column(name = "detail", nullable = false, columnDefinition = "TEXT")
    // 상세 내용
    private String detail;

    @Column(name = "created_at", nullable = false)
    // 기록 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
