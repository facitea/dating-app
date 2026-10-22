/**
 * 로그인 실패 횟수와 잠금 상태를 저장하는 JPA 엔티티
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
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 로그인 시도 ID
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    // 로그인 식별자: 현재는 이메일
    private String loginId;

    @Column(nullable = false)
    // 연속 실패 횟수
    @Builder.Default
    private Integer failureCount = 0;

    @Column
    // 잠금 해제 예정 시각
    private LocalDateTime lockedUntil;

    @Column(nullable = false)
    // 마지막 실패 시각
    @Builder.Default
    private LocalDateTime lastFailedAt = LocalDateTime.now();

    @Column
    // 마지막 성공 시각
    private LocalDateTime lastSucceededAt;
}
