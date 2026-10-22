/**
 * ProfileService 비즈니스 로직
 */
package com.dating.backend.service;

import com.dating.backend.dto.MyProfileResponse;
import com.dating.backend.dto.UpdateMyProfileRequest;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserVerification;
import com.dating.backend.repository.UserProfileImageRepository;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserRepository;
import com.dating.backend.repository.UserVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final com.dating.backend.matching.ProfileModeration moderation;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final UserProfileImageRepository userProfileImageRepository;
    private final AccountReviewPolicyService accountReviewPolicyService;
    private final FraudDetectionService fraudDetectionService;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(String email) {
        User user = getUserByEmail(email);
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        UserVerification verification = userVerificationRepository.findByUserId(user.getId()).orElse(null);

        return buildProfileResponse(user, profile, verification);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(String email, UpdateMyProfileRequest request) {
        User user = getUserByEmail(email);
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().userId(user.getId()).build());
        UserVerification verification = userVerificationRepository.findByUserId(user.getId())
                .orElseGet(() -> UserVerification.builder().userId(user.getId()).build());

        moderation.beforeEdit(user);
        user.setNickname(request.getNickname());

        profile.setRegion(request.getRegion());
        profile.setJob(emptyToNull(request.getJob()));
        profile.setMbti(emptyToNull(request.getMbti()));
        profile.setPersonality(emptyToNull(request.getPersonality()));
        profile.setIdealType(emptyToNull(request.getIdealType()));
        profile.setIntroduction(emptyToNull(request.getIntroduction()));
        profile.setSmokingStatus(request.getSmokingStatus());
        profile.setDrinkingStatus(request.getDrinkingStatus());
        profile.setReligion(request.getReligion());
        profile.setUpdatedAt(LocalDateTime.now());

        verification.setBirthDate(request.getBirthDate());
        verification.setGender(request.getGender());
        if (verification.getIsVerified() == null) {
            verification.setIsVerified(false);
        }
        verification.setUpdatedAt(LocalDateTime.now());

        if (accountReviewPolicyService.isProfileComplete(profile, verification)) {
            if (user.getProfileCompletedAt() == null) {
                user.setProfileCompletedAt(LocalDateTime.now());
            }

            long imageCount = userProfileImageRepository.countByUserId(user.getId());
            if ("PENDING_REVIEW".equals(user.getStatus())) {
                user.setReviewComment(imageCount >= 2
                        ? "프로필과 사진 등록이 완료되어 심사 대기 중입니다."
                        : "프로필은 완료되었지만 사진이 아직 부족합니다. 대표 사진 포함 최소 2장을 등록해 주세요.");
            }
        } else {
            user.setProfileCompletedAt(null);
            if ("PENDING_REVIEW".equals(user.getStatus()) || "REJECTED".equals(user.getStatus())) {
                user.setReviewComment("프로필 필수 항목을 모두 입력해야 다시 심사를 진행할 수 있습니다.");
            }
        }

        userRepository.save(user);
        userProfileRepository.save(profile);
        userVerificationRepository.save(verification);
        fraudDetectionService.evaluateUserProfile(user.getId());

        return buildProfileResponse(user, profile, verification);
    }

    private MyProfileResponse buildProfileResponse(User user, UserProfile profile, UserVerification verification) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getNickname(),
                user.getStatus(),
                user.getProvider(),
                user.getReviewComment(),
                verification != null ? verification.getBirthDate() : null,
                verification != null ? verification.getGender() : null,
                verification != null ? verification.getIsVerified() : Boolean.FALSE,
                verification != null ? verification.getVerifiedAt() : null,
                profile != null ? defaultString(profile.getRegion()) : "",
                profile != null ? defaultString(profile.getJob()) : "",
                profile != null ? defaultString(profile.getMbti()) : "",
                profile != null ? defaultString(profile.getPersonality()) : "",
                profile != null ? defaultString(profile.getIdealType()) : "",
                profile != null ? defaultString(profile.getIntroduction()) : "",
                profile != null ? defaultString(profile.getSmokingStatus()) : "NON_SMOKER",
                profile != null ? defaultString(profile.getDrinkingStatus()) : "NONE",
                profile != null ? defaultString(profile.getReligion()) : "NONE"
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."));
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
