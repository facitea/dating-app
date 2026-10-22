/**
 * 로그인 실패 제한과 임시 잠금을 처리하는 서비스
 */
package com.dating.backend.service;

import com.dating.backend.entity.LoginAttempt;
import com.dating.backend.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final int LOCK_MINUTES = 10;

    private final LoginAttemptRepository loginAttemptRepository;

    @Transactional(readOnly = true)
    public void validateNotLocked(String loginId) {
        String normalizedLoginId = normalizeLoginId(loginId);
        loginAttemptRepository.findByLoginId(normalizedLoginId)
                .filter(this::isLocked)
                .ifPresent(attempt -> {
                    long remainingMinutes = Math.max(1, java.time.Duration.between(LocalDateTime.now(), attempt.getLockedUntil()).toMinutes() + 1);
                    throw new ResponseStatusException(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "로그인 실패 횟수가 많아 계정이 잠시 잠겼습니다. " + remainingMinutes + "분 뒤 다시 시도해 주세요."
                    );
                });
    }

    @Transactional
    public void recordFailure(String loginId) {
        String normalizedLoginId = normalizeLoginId(loginId);
        LoginAttempt attempt = loginAttemptRepository.findByLoginId(normalizedLoginId)
                .orElseGet(() -> LoginAttempt.builder().loginId(normalizedLoginId).build());

        int nextFailureCount = attempt.getFailureCount() + 1;
        attempt.setFailureCount(nextFailureCount);
        attempt.setLastFailedAt(LocalDateTime.now());

        if (nextFailureCount >= MAX_FAILURES) {
            attempt.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }

        loginAttemptRepository.save(attempt);
    }

    @Transactional
    public void recordSuccess(String loginId) {
        String normalizedLoginId = normalizeLoginId(loginId);
        loginAttemptRepository.findByLoginId(normalizedLoginId)
                .ifPresent(attempt -> {
                    attempt.setFailureCount(0);
                    attempt.setLockedUntil(null);
                    attempt.setLastSucceededAt(LocalDateTime.now());
                    loginAttemptRepository.save(attempt);
                });
    }

    private boolean isLocked(LoginAttempt attempt) {
        return attempt.getLockedUntil() != null && attempt.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private String normalizeLoginId(String loginId) {
        return loginId == null ? "" : loginId.trim().toLowerCase(Locale.ROOT);
    }
}
