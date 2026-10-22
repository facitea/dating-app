/**
 * IP, 이메일, 전화번호별 요청 횟수 제한 상태를 저장하는 JPA 엔티티
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
@Table(name = "rate_limit_buckets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 요청 제한 버킷 ID
    private Long id;

    @Column(nullable = false, length = 60)
    // 제한할 액션 종류
    private String actionType;

    @Column(nullable = false, length = 30)
    // 식별자 종류
    private String identifierType;

    @Column(nullable = false, length = 255)
    // 식별자 값
    private String identifierValue;

    @Column(nullable = false)
    // 제한 창 시작 시각
    private LocalDateTime windowStartedAt;

    @Column(nullable = false)
    // 현재 제한 창 안의 요청 횟수
    @Builder.Default
    private Integer requestCount = 0;

    @Column
    // 차단 해제 예정 시각
    private LocalDateTime blockedUntil;

    @Column(nullable = false)
    // 생성 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    // 수정 시각
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
