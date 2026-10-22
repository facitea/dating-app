/**
 * 고객센터 문의 등록 요청 DTO입니다.
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportInquiryCreateRequest {

    // 문의 분류
    @NotBlank(message = "문의 분류를 선택해 주세요.")
    private String category;

    // 문의 경로
    @NotBlank(message = "문의 경로 정보가 필요합니다.")
    private String channel;

    // 문의 내용
    @NotBlank(message = "문의 내용을 입력해 주세요.")
    @Size(max = 1000, message = "문의 내용은 1000자 이하로 입력해 주세요.")
    private String question;
}
