/**
 * 관리자 고객 문의 목록 응답 DTO입니다.
 */
package com.dating.backend.dto;

import com.dating.backend.entity.SupportInquiry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminSupportInquiryResponse {

    private Long id;
    private Long userId;
    private String email;
    private String nickname;
    private String category;
    private String channel;
    private String status;
    private String question;
    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminSupportInquiryResponse from(SupportInquiry inquiry) {
        return new AdminSupportInquiryResponse(
                inquiry.getId(),
                inquiry.getUserId(),
                inquiry.getEmail(),
                inquiry.getNickname(),
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
