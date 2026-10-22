/**
 * 자체 인증번호 발급, 확인, 회원가입 인증 검증을 담당하는 서비스
 */
package com.dating.backend.service;

import com.dating.backend.dto.AuthVerificationConfirmRequest;
import com.dating.backend.dto.AuthVerificationConfirmResponse;
import com.dating.backend.dto.AuthVerificationRequest;
import com.dating.backend.dto.AuthVerificationStartResponse;
import com.dating.backend.entity.AuthVerification;
import com.dating.backend.repository.AuthVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthVerificationService {

    private static final int CODE_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final AuthVerificationRepository authVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthVerificationStartResponse requestCode(AuthVerificationRequest request) {
        String targetType = normalizeType(request.getTargetType());
        String purpose = normalizePurpose(request.getPurpose());
        String targetValue = normalizeTargetValue(targetType, request.getTargetValue());

        validateResendCooldown(targetType, targetValue, purpose);

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES);

        authVerificationRepository.save(AuthVerification.builder()
                .targetType(targetType)
                .targetValue(targetValue)
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(expiresAt)
                .build());

        log.info("[DEV AUTH VERIFICATION] targetType={}, targetValue={}, purpose={}, code={}, expiresAt={}",
                targetType, targetValue, purpose, code, expiresAt);

        return new AuthVerificationStartResponse(
                "인증번호를 발급했습니다. 개발 환경에서는 서버 로그의 [DEV AUTH VERIFICATION] 항목에서 확인할 수 있습니다.",
                expiresAt,
                code
        );
    }

    @Transactional
    public AuthVerificationConfirmResponse confirmCode(AuthVerificationConfirmRequest request) {
        String targetType = normalizeType(request.getTargetType());
        String purpose = normalizePurpose(request.getPurpose());
        String targetValue = normalizeTargetValue(targetType, request.getTargetValue());

        AuthVerification verification = authVerificationRepository
                .findTopByTargetTypeAndTargetValueAndPurposeOrderByCreatedAtDesc(targetType, targetValue, purpose)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 요청 기록을 찾을 수 없습니다."));

        validateCanConfirm(verification);

        verification.setAttemptCount(verification.getAttemptCount() + 1);
        if (!passwordEncoder.matches(request.getCode(), verification.getCodeHash())) {
            authVerificationRepository.save(verification);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다.");
        }

        String verificationToken = UUID.randomUUID().toString();
        verification.setVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        verification.setVerificationToken(verificationToken);
        authVerificationRepository.save(verification);

        return new AuthVerificationConfirmResponse("인증이 완료되었습니다.", verificationToken);
    }

    @Transactional(readOnly = true)
    public void validateSignupEmailVerified(String email, String verificationToken) {
        String normalizedEmail = normalizeTargetValue("EMAIL", email);
        AuthVerification verification = authVerificationRepository
                .findByTargetTypeAndTargetValueAndPurposeAndVerificationTokenAndVerifiedTrue(
                        "EMAIL",
                        normalizedEmail,
                        "SIGNUP",
                        verificationToken
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 인증을 먼저 완료해 주세요."));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 인증 시간이 만료되었습니다. 다시 인증해 주세요.");
        }
    }

    private void validateResendCooldown(String targetType, String targetValue, String purpose) {
        authVerificationRepository.findTopByTargetTypeAndTargetValueAndPurposeOrderByCreatedAtDesc(targetType, targetValue, purpose)
                .ifPresent(previous -> {
                    LocalDateTime availableAt = previous.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
                    if (availableAt.isAfter(LocalDateTime.now())) {
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 뒤 다시 요청할 수 있습니다.");
                    }
                });
    }

    private void validateCanConfirm(AuthVerification verification) {
        if (Boolean.TRUE.equals(verification.getVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 인증이 완료된 요청입니다.");
        }
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 시간이 만료되었습니다. 다시 요청해 주세요.");
        }
        if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 시도 횟수를 초과했습니다. 다시 요청해 주세요.");
        }
    }

    private String normalizeType(String targetType) {
        return targetType == null ? "" : targetType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePurpose(String purpose) {
        return purpose == null ? "" : purpose.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTargetValue(String targetType, String targetValue) {
        String value = targetValue == null ? "" : targetValue.trim();
        if ("EMAIL".equals(targetType)) {
            return value.toLowerCase(Locale.ROOT);
        }
        if ("PHONE".equals(targetType)) {
            return value.replaceAll("[^0-9]", "");
        }
        return value;
    }
}
