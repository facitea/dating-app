/**
 * 인증/보안 관련 이상 요청과 차단 이력을 저장하는 JPA 엔티티
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
@Table(name = "security_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 보안 이벤트 ID
    private Long id;

    @Column(nullable = false, length = 50)
    // 이벤트 종류
    private String eventType;

    @Column(nullable = false, length = 30)
    // 식별자 종류
    private String identifierType;

    @Column(nullable = false, length = 255)
    // 식별자 값
    private String identifierValue;

    @Column(length = 80)
    // 요청 IP 주소
    private String ipAddress;

    @Column(length = 500)
    // 요청 User-Agent
    private String userAgent;

    @Column(nullable = false, length = 500)
    // 기록 사유
    private String reason;

    @Column(nullable = false)
    // 위험 점수
    @Builder.Default
    private Integer riskScore = 0;

    @Column(nullable = false)
    // 생성 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
