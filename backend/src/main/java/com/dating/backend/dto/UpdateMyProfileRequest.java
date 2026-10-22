/**
 * UpdateMyProfileRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateMyProfileRequest {

    // 닉네임
    @NotBlank
    @Size(max = 30)
    private String nickname;

    @NotNull
    // 생년월일
    private LocalDate birthDate;

    @NotBlank
    @Pattern(regexp = "MALE|FEMALE")
    // 성별
    private String gender;

    @NotBlank
    @Size(max = 50)
    // 거주 지역
    private String region;

    @Size(max = 100)
    // 직업
    private String job;

    @Pattern(
            regexp = "^$|INTJ|INTP|ENTJ|ENTP|INFJ|INFP|ENFJ|ENFP|ISTJ|ISFJ|ESTJ|ESFJ|ISTP|ISFP|ESTP|ESFP"
    )
    // MBTI
    private String mbti;

    // 성격
    private String personality;
    // 이상형
    private String idealType;
    // 자기소개
    private String introduction;

    @NotBlank
    @Pattern(regexp = "NON_SMOKER|SMOKER|OCCASIONAL")
    // 흡연 여부
    private String smokingStatus;

    @NotBlank
    @Pattern(regexp = "NONE|SOMETIMES|OFTEN")
    // 음주 여부
    private String drinkingStatus;

    @NotBlank
    @Pattern(regexp = "NONE|CHRISTIAN|BUDDHIST|CATHOLIC|OTHER")
    // 종교
    private String religion;
}
