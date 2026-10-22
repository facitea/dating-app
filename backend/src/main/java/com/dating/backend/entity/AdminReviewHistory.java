/**
 * AdminReviewHistory JPA 엔티티
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
@Table(name = "admin_review_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 이력 ID
    private Long id;

    @Column(name = "user_id", nullable = false)
    // 유저 ID
    private Long userId;

    @Column(name = "action_type", nullable = false, length = 30)
    // 조치 유형
    private String actionType;

    @Column(name = "detail", nullable = false, columnDefinition = "TEXT")
    // 조치 상세
    private String detail;

    @Column(name = "actor_label", nullable = false, length = 50)
    // 처리자 라벨
    @Builder.Default
    private String actorLabel = "ADMIN";

    @Column(name = "created_at", nullable = false)
    // 생성 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
