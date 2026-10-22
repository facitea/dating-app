/**
 * AdminReviewHistoryResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminReviewHistoryResponse {

    // 이력 ID
    private Long id;
    // 조치 유형
    private String actionType;
    // 조치 상세
    private String detail;
    // 처리자
    private String actorLabel;
    // 처리 시각
    private LocalDateTime createdAt;
}
