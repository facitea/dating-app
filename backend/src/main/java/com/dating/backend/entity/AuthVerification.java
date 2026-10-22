/**
 * 자체 인증번호 요청과 확인 상태를 저장하는 JPA 엔티티
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
@Table(name = "auth_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 인증 요청 ID
    private Long id;

    @Column(nullable = false, length = 20)
    // 인증 대상 종류: EMAIL, PHONE
    private String targetType;

    @Column(nullable = false, length = 255)
    // 인증 대상 값: 이메일 주소 또는 전화번호
    private String targetValue;

    @Column(nullable = false, length = 30)
    // 인증 목적: SIGNUP, LOGIN, PASSWORD_RESET
    private String purpose;

    @Column(nullable = false, length = 255)
    // 인증번호 해시
    private String codeHash;

    @Column(length = 255)
    // 인증 완료 후 회원가입 등에 사용할 확인 토큰
    private String verificationToken;

    @Column(nullable = false)
    // 인증번호 만료 시각
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    // 인증 완료 여부
    @Builder.Default
    private Boolean verified = false;

    @Column
    // 인증 완료 시각
    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    // 인증번호 확인 시도 횟수
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(nullable = false)
    // 요청 생성 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
