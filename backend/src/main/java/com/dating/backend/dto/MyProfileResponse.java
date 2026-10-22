/**
 * 내 프로필 조회 응답 DTO입니다.
 */
package com.dating.backend.dto;

import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserVerification;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyProfileResponse {

    // 사용자 ID
    private Long userId;
    // 이메일
    private String email;
    // 전화번호
    private String phone;
    // 닉네임
    private String nickname;
    // 계정 상태
    private String status;
    // 가입 경로
    private String provider;
    // 심사 안내 문구
    private String reviewComment;
    // 생년월일
    private LocalDate birthDate;
    // 성별
    private String gender;
    // 본인 인증 여부
    private Boolean isVerified;
    // 본인 인증 시각
    private LocalDateTime verifiedAt;
    // 거주 지역
    private String region;
    // 직업
    private String job;
    // MBTI
    private String mbti;
    // 성격
    private String personality;
    // 이상형
    private String idealType;
    // 자기소개
    private String introduction;
    // 흡연 여부
    private String smokingStatus;
    // 음주 여부
    private String drinkingStatus;
    // 종교
    private String religion;

    public static MyProfileResponse from(User user, UserProfile profile, UserVerification verification) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getNickname(),
                user.getStatus(),
                user.getProvider(),
                user.getReviewComment(),
                verification.getBirthDate(),
                verification.getGender(),
                verification.getIsVerified(),
                verification.getVerifiedAt(),
                profile.getRegion(),
                profile.getJob(),
                profile.getMbti(),
                profile.getPersonality(),
                profile.getIdealType(),
                profile.getIntroduction(),
                profile.getSmokingStatus(),
                profile.getDrinkingStatus(),
                profile.getReligion()
        );
    }
}
