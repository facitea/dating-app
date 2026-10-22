/**
 * UserVerification JPA 엔티티
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 인증 ID
    private Long id;

    @Column(nullable = false, unique = true)
    // 유저 ID
    private Long userId;

    @Column(nullable = false)
    // 생년월일
    private LocalDate birthDate;

    @Column(nullable = false)
    // 성별
    private String gender;

    @Column(nullable = false)
    // 인증 완료 여부
    @Builder.Default
    private Boolean isVerified = false;

    @Column
    // 인증 완료 시각
    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    // 생성 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    // 수정 시각
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
