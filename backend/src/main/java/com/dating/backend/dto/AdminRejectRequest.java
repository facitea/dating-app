/**
 * AdminRejectRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRejectRequest {

    @NotBlank
    @Size(max = 255)
    // 반려 사유
    private String reviewComment;
}
