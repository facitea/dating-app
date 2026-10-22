/**
 * AccountReviewPolicyService 비즈니스 로직
 */
package com.dating.backend.service;

import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserVerification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AccountReviewPolicyService {

    private static final int SIGNUP_REVIEW_DEADLINE_DAYS = 3;
    private static final int REJECT_RETRY_DEADLINE_DAYS = 7;

    public LocalDateTime createSignupDeadline() {
        return LocalDateTime.now().plusDays(SIGNUP_REVIEW_DEADLINE_DAYS);
    }

    public LocalDateTime createRejectedDeadline() {
        return LocalDateTime.now().plusDays(REJECT_RETRY_DEADLINE_DAYS);
    }

    public boolean isProfileComplete(UserProfile profile, UserVerification verification) {
        return hasText(profile.getRegion())
                && hasText(profile.getJob())
                && hasText(profile.getMbti())
                && hasText(profile.getPersonality())
                && hasText(profile.getIdealType())
                && hasText(profile.getIntroduction())
                && hasText(profile.getSmokingStatus())
                && hasText(profile.getDrinkingStatus())
                && hasText(profile.getReligion())
                && verification.getBirthDate() != null
                && verification.getBirthDate().isBefore(LocalDate.now())
                && hasText(verification.getGender());
    }

    public long calculateRemainingDays(LocalDateTime reviewDeadlineAt) {
        if (reviewDeadlineAt == null) {
            return -1;
        }

        long days = ChronoUnit.DAYS.between(LocalDate.now(), reviewDeadlineAt.toLocalDate());
        return Math.max(days, 0);
    }

    public String buildDeadlineWarning(LocalDateTime reviewDeadlineAt) {
        if (reviewDeadlineAt == null) {
            return null;
        }

        long remainingDays = calculateRemainingDays(reviewDeadlineAt);
        if (remainingDays == 0) {
            return "심사 기한이 오늘까지입니다. 프로필과 사진 등록을 완료하지 않으면 계정이 삭제될 수 있습니다.";
        }
        return "심사 기한이 " + remainingDays + "일 남았습니다. 프로필과 사진 등록을 완료하지 않으면 계정이 삭제될 수 있습니다.";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
