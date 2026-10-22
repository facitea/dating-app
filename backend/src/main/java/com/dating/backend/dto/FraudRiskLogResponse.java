/**
 * FraudRiskLogResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FraudRiskLogResponse {

    // 로그 ID
    private Long id;
    // 위험 유형
    private String riskType;
    // 점수
    private Integer score;
    // 상세 내용
    private String detail;
    // 기록 시각
    private LocalDateTime createdAt;
}
