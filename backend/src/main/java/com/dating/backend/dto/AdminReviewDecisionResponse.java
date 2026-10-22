/**
 * AdminReviewDecisionResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminReviewDecisionResponse {

    // 유저 ID
    private Long userId;
    // 심사 상태
    private String status;
    // 심사 코멘트
    private String reviewComment;
    // 응답 메시지
    private String message;
}
