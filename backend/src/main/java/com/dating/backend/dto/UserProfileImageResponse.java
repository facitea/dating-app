/**
 * UserProfileImageResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import com.dating.backend.entity.UserProfileImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileImageResponse {

    // 사진 ID
    private Long id;
    // 이미지 URL
    private String imageUrl;
    // 사진 순서
    private Integer imageOrder;
    // 대표 사진 여부
    private Boolean isMain;

    public static UserProfileImageResponse from(UserProfileImage image) {
        return new UserProfileImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getImageOrder(),
                image.getIsMain()
        );
    }
}
