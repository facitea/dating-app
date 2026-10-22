/**
 * AdminRiskActionRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRiskActionRequest {

    @NotBlank(message = "조치 유형을 입력해 주세요.")
    // 조치 유형
    private String action;

    @NotBlank(message = "운영자 메모를 입력해 주세요.")
    // 운영자 메모
    private String adminNote;
}
