/**
 * ReviewStatusResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewStatusResponse {

    // 심사 상태
    private String status;
    // 등록된 사진 수
    private int imageCount;
    // 심사 가능 여부
    private boolean readyForReview;
    // 프로필 완료 여부
    private boolean profileComplete;
    // 심사 코멘트
    private String reviewComment;
    // 응답 메시지
    private String message;
    // 심사 마감일
    private LocalDateTime reviewDeadlineAt;
    // 남은 심사일수
    private long remainingDays;
}
