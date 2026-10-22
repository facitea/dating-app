/**
 * 로그인 유지용 refresh token을 저장하는 JPA 엔티티
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
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // refresh token ID
    private Long id;

    @Column(nullable = false)
    // 사용자 ID
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    // refresh token 해시
    private String tokenHash;

    @Column(nullable = false)
    // 만료 시각
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    // 폐기 여부
    @Builder.Default
    private Boolean revoked = false;

    @Column
    // 폐기 시각
    private LocalDateTime revokedAt;

    @Column(nullable = false)
    // 생성 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
