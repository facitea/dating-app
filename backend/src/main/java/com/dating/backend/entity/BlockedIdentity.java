/**
 * BlockedIdentity JPA 엔티티
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
@Table(name = "blocked_identities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 차단 ID
    private Long id;

    @Column(name = "identity_type", nullable = false, length = 30)
    // 차단 유형
    private String identityType;

    @Column(name = "identity_value", nullable = false, length = 255)
    // 차단 값
    private String identityValue;

    @Column(name = "reason", nullable = false, length = 255)
    // 차단 사유
    private String reason;

    @Column(name = "source_user_id")
    // 차단 근거 유저 ID
    private Long sourceUserId;

    @Column(name = "active", nullable = false)
    // 활성 여부
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    // 등록 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    // 변경 시각
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
