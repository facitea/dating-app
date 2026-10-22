/**
 * ResolveReportRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveReportRequest {
    private String action = "NONE";
    private Integer days;
    private String publicReason;


    @NotBlank(message = "처리 메모를 입력해 주세요.")
    // 운영자 메모
    private String adminNote;
}
