/**
 * SignupRequest 요청/응답 DTO
 */
package com.dating.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {

    @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
    @NotBlank(message = "이메일을 입력해 주세요.")
    // 이메일
    private String email;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하로 입력해 주세요.")
    // 닉네임
    private String nickname;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해 주세요.")
    // 비밀번호
    private String password;

    @NotBlank(message = "이메일 인증을 완료해 주세요.")
    // 이메일 인증 완료 토큰
    private String emailVerificationToken;

    @NotNull(message = "생년월일을 입력해 주세요.")
    // 생년월일
    private LocalDate birthDate;

    @NotBlank(message = "성별을 선택해 주세요.")
    @Pattern(regexp = "MALE|FEMALE", message = "성별은 MALE 또는 FEMALE로 입력해 주세요.")
    // 성별
    private String gender;

    @NotBlank(message = "거주 지역을 입력해 주세요.")
    @Size(max = 50, message = "거주 지역은 50자 이하로 입력해 주세요.")
    // 거주 지역
    private String region;

    @Size(max = 100, message = "직업은 100자 이하로 입력해 주세요.")
    // 직업
    private String job;

    @Pattern(
            regexp = "^$|INTJ|INTP|ENTJ|ENTP|INFJ|INFP|ENFJ|ENFP|ISTJ|ISFJ|ESTJ|ESFJ|ISTP|ISFP|ESTP|ESFP",
            message = "MBTI는 올바른 형식으로 입력해 주세요."
    )
    // MBTI
    private String mbti;

    // 성격
    private String personality;
    // 이상형
    private String idealType;
    // 자기소개
    private String introduction;

    @NotBlank(message = "흡연 여부를 선택해 주세요.")
    @Pattern(regexp = "NON_SMOKER|SMOKER|OCCASIONAL", message = "흡연 여부는 NON_SMOKER, SMOKER, OCCASIONAL 중 하나로 입력해 주세요.")
    // 흡연 여부
    private String smokingStatus;

    @NotBlank(message = "음주 여부를 선택해 주세요.")
    @Pattern(regexp = "NONE|SOMETIMES|OFTEN", message = "음주 여부는 NONE, SOMETIMES, OFTEN 중 하나로 입력해 주세요.")
    // 음주 여부
    private String drinkingStatus;

    @NotBlank(message = "종교를 선택해 주세요.")
    @Pattern(regexp = "NONE|CHRISTIAN|BUDDHIST|CATHOLIC|OTHER", message = "종교는 NONE, CHRISTIAN, BUDDHIST, CATHOLIC, OTHER 중 하나로 입력해 주세요.")
    // 종교
    private String religion;

    @AssertTrue(message = "약관에 동의해 주세요.")
    // 약관 동의 여부
    private boolean agreedToTerms;
    @AssertTrue(message="개인정보 수집·이용에 동의해 주세요.") private boolean agreedToPrivacy;
    @AssertTrue(message="만 19세 이상만 가입할 수 있습니다.") private boolean adultConfirmed;
    private boolean marketingConsent;
    @NotBlank private String termsVersion;

    // 사람이 보지 못하는 봇 방지용 숨김 필드
    private String website;

    // 회원가입 화면 진입 시각
    private Long formStartedAt;
}
