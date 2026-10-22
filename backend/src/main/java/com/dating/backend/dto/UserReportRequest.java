/**
 * UserReportRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserReportRequest {

    @NotNull(message = "신고 대상 유저 ID를 입력해 주세요.")
    // 신고 대상 유저 ID
    private Long reportedUserId;

    @NotBlank(message = "신고 사유 코드를 입력해 주세요.")
    // 신고 사유 코드
    private String reasonCode;

    @NotBlank(message = "신고 상세 내용을 입력해 주세요.")
    // 신고 상세
    private String detail;
}
