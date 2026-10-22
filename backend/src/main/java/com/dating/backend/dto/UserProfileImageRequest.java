/**
 * UserProfileImageRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileImageRequest {

    @NotBlank
    @Size(max = 500)
    // 이미지 URL
    private String imageUrl;

    @NotNull
    @Min(0)
    @Max(6)
    // 사진 순서
    private Integer imageOrder;

    @NotNull
    // 대표 사진 여부
    private Boolean isMain;
}
