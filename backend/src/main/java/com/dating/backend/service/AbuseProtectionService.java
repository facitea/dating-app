/**
 * 자체 인증서비스 앞단에서 봇/남용 요청을 제한하는 서비스
 */
package com.dating.backend.service;

import com.dating.backend.entity.RateLimitBucket;
import com.dating.backend.repository.RateLimitBucketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AbuseProtectionService {

    private final RateLimitBucketRepository rateLimitBucketRepository;
    private final SecurityEventService securityEventService;

    // 로그인 요청 전 IP 기준 과도한 시도를 제한한다.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void protectLogin(String email, AbuseRequestContext context) {
        checkRateLimit("LOGIN", "IP", context.ipAddress(), 30, Duration.ofMinutes(10), Duration.ofMinutes(10), context);
        checkSuspiciousUserAgent("LOGIN", email, context);
    }

    // 이메일 인증번호 요청 전 IP와 이메일 기준 과도한 요청을 제한한다.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void protectEmailVerification(String email, AbuseRequestContext context) {
        String normalizedEmail = normalizeEmail(email);
        checkRateLimit("EMAIL_VERIFICATION", "IP", context.ipAddress(), 10, Duration.ofMinutes(10), Duration.ofMinutes(10), context);
        checkRateLimit("EMAIL_VERIFICATION", "EMAIL", normalizedEmail, 3, Duration.ofMinutes(10), Duration.ofMinutes(10), context);
        checkSuspiciousUserAgent("EMAIL_VERIFICATION", normalizedEmail, context);
    }

    // 전화번호 인증번호 요청 전 IP 기준 과도한 요청을 제한한다.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void protectPhoneVerification(String phone, AbuseRequestContext context) {
        String normalizedPhone = normalizePhone(phone);
        checkRateLimit("PHONE_VERIFICATION", "IP", context.ipAddress(), 10, Duration.ofMinutes(10), Duration.ofMinutes(10), context);
        checkRateLimit("PHONE_VERIFICATION", "PHONE", normalizedPhone, 3, Duration.ofMinutes(10), Duration.ofMinutes(10), context);
        checkSuspiciousUserAgent("PHONE_VERIFICATION", normalizedPhone, context);
    }

    // 회원가입 제출 전 허니팟, 제출 속도, IP 기준 과도한 가입 시도를 제한한다.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void protectSignup(String email, String honeypot, Long formStartedAt, AbuseRequestContext context) {
        String normalizedEmail = normalizeEmail(email);
        checkRateLimit("SIGNUP", "IP", context.ipAddress(), 5, Duration.ofMinutes(10), Duration.ofMinutes(30), context);
        checkSuspiciousUserAgent("SIGNUP", normalizedEmail, context);

        if (honeypot != null && !honeypot.isBlank()) {
            securityEventService.record("HONEYPOT_BLOCKED", "EMAIL", normalizedEmail, context, "숨겨진 봇 방지 필드에 값이 입력되었습니다.", 90);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        }

        if (formStartedAt != null) {
            long elapsedMillis = Instant.now().toEpochMilli() - formStartedAt;
            if (elapsedMillis >= 0 && elapsedMillis < 2_000) {
                securityEventService.record("FAST_SIGNUP_BLOCKED", "EMAIL", normalizedEmail, context, "회원가입 제출 속도가 비정상적으로 빠릅니다.", 70);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "입력 확인 시간이 너무 짧습니다. 잠시 후 다시 시도해 주세요.");
            }
        }
    }

    // 비밀번호 재설정 요청 전 IP와 이메일 기준 과도한 요청을 제한한다.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void protectPasswordReset(String email, AbuseRequestContext context) {
        String normalizedEmail = normalizeEmail(email);
        checkRateLimit("PASSWORD_RESET", "IP", context.ipAddress(), 5, Duration.ofMinutes(10), Duration.ofMinutes(10), context);
        checkRateLimit("PASSWORD_RESET", "EMAIL", normalizedEmail, 3, Duration.ofMinutes(30), Duration.ofMinutes(30), context);
        checkSuspiciousUserAgent("PASSWORD_RESET", normalizedEmail, context);
    }

    // 인증 실패 이력을 보안 이벤트로 기록한다.
    public void recordFailure(String eventType, String identifierType, String identifierValue, AbuseRequestContext context, String reason) {
        securityEventService.record(eventType, identifierType, identifierValue, context, reason, 40);
    }

    private void checkRateLimit(
            String actionType,
            String identifierType,
            String identifierValue,
            int maxRequests,
            Duration window,
            Duration blockDuration,
            AbuseRequestContext context
    ) {
        String safeIdentifier = normalizeIdentifier(identifierValue);
        LocalDateTime now = LocalDateTime.now();
        RateLimitBucket bucket = rateLimitBucketRepository
                .findByActionTypeAndIdentifierTypeAndIdentifierValue(actionType, identifierType, safeIdentifier)
                .orElseGet(() -> RateLimitBucket.builder()
                        .actionType(actionType)
                        .identifierType(identifierType)
                        .identifierValue(safeIdentifier)
                        .windowStartedAt(now)
                        .requestCount(0)
                        .build());

        if (bucket.getBlockedUntil() != null && bucket.getBlockedUntil().isAfter(now)) {
            securityEventService.record(actionType + "_BLOCKED", identifierType, safeIdentifier, context, "요청 제한으로 차단된 상태입니다.", 80);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }

        if (bucket.getWindowStartedAt() == null || bucket.getWindowStartedAt().plus(window).isBefore(now)) {
            bucket.setWindowStartedAt(now);
            bucket.setRequestCount(0);
            bucket.setBlockedUntil(null);
        }

        bucket.setRequestCount(bucket.getRequestCount() + 1);
        bucket.setUpdatedAt(now);

        if (bucket.getRequestCount() > maxRequests) {
            bucket.setBlockedUntil(now.plus(blockDuration));
            rateLimitBucketRepository.save(bucket);
            securityEventService.record(actionType + "_RATE_LIMITED", identifierType, safeIdentifier, context, "허용된 요청 횟수를 초과했습니다.", 85);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }

        rateLimitBucketRepository.save(bucket);
    }

    private void checkSuspiciousUserAgent(String actionType, String identifierValue, AbuseRequestContext context) {
        if (context.userAgent() == null || context.userAgent().isBlank()) {
            securityEventService.record(actionType + "_SUSPICIOUS_AGENT", "UNKNOWN", identifierValue, context, "User-Agent가 비어 있습니다.", 25);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private String normalizeIdentifier(String identifierValue) {
        return identifierValue == null || identifierValue.isBlank() ? "UNKNOWN" : identifierValue;
    }
}
