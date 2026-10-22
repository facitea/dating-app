/**
 * 고객센터 문의 응답 DTO입니다.
 */
package com.dating.backend.dto;

import com.dating.backend.entity.SupportInquiry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SupportInquiryResponse {

    private Long id;
    private String category;
    private String channel;
    private String status;
    private String question;
    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupportInquiryResponse from(SupportInquiry inquiry) {
        return new SupportInquiryResponse(
                inquiry.getId(),
                inquiry.getCategory(),
                inquiry.getChannel(),
                inquiry.getStatus(),
                inquiry.getQuestion(),
                inquiry.getAdminReply(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }
}
