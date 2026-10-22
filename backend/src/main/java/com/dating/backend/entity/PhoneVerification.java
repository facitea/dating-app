/**
 * PhoneVerification JPA 엔티티
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
@Table(name = "phone_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 인증 ID
    private Long id;

    @Column(nullable = false, length = 20)
    // 휴대폰 번호
    private String phone;

    @Column(nullable = false, length = 10)
    // 인증 코드
    private String code;

    @Column(nullable = false)
    // 만료 시각
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    // 인증 성공 여부
    @Builder.Default
    private Boolean verified = false;

    @Column
    // 인증 완료 시각
    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    // 시도 횟수
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(nullable = false)
    // 요청 생성 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
