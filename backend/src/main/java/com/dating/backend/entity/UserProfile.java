/**
 * UserProfile JPA 엔티티
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
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 프로필 ID
    private Long id;

    @Column(nullable = false, unique = true)
    // 유저 ID
    private Long userId;

    @Column(nullable = false)
    // 거주 지역
    private String region;

    @Column
    // 직업
    private String job;

    @Column(length = 4)
    // MBTI
    private String mbti;

    @Column(columnDefinition = "TEXT")
    // 성격
    private String personality;

    @Column(columnDefinition = "TEXT")
    // 이상형
    private String idealType;

    @Column(columnDefinition = "TEXT")
    // 자기소개
    private String introduction;

    @Column(nullable = false)
    // 흡연 여부
    @Builder.Default
    private String smokingStatus = "NON_SMOKER";

    @Column(nullable = false)
    // 음주 여부
    @Builder.Default
    private String drinkingStatus = "NONE";

    @Column(nullable = false)
    // 종교
    @Builder.Default
    private String religion = "NONE";

    @Column(nullable = false)
    // 생성 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    // 수정 시각
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
