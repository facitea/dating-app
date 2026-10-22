/**
 * UserProfileImage JPA 엔티티
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
@Table(name = "user_profile_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 사진 ID
    private Long id;

    @Column(nullable = false)
    // 유저 ID
    private Long userId;

    @Column(nullable = false, length = 500)
    // 이미지 URL
    private String imageUrl;

    @Column(nullable = false)
    // 사진 순서
    private Integer imageOrder;

    @Column(nullable = false)
    // 대표 사진 여부
    @Builder.Default
    private Boolean isMain = false;

    @Column(nullable = false)
    // 등록 시각
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
