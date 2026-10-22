/**
 * UserResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import com.dating.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponse {

    // 유저 ID
    private Long id;
    // 이메일
    private String email;
    // 닉네임
    private String nickname;
    // 가입 경로
    private String provider;
    // 계정 상태
    private String status;
    // 심사 마감일
    private LocalDateTime reviewDeadlineAt;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider(),
                user.getStatus(),
                user.getReviewDeadlineAt()
        );
    }
}
