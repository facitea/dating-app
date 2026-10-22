/**
 * 관리자 고객 문의 처리 요청 DTO입니다.
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSupportInquiryUpdateRequest {

    @NotBlank(message = "문의 상태를 선택해 주세요.")
    private String status;

    @Size(max = 1000, message = "운영자 답변은 1000자 이하로 입력해 주세요.")
    private String adminReply;
}
