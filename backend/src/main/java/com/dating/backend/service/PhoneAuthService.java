/**
 * 전화번호 인증과 전화번호 기반 로그인/연결을 처리합니다.
 */
package com.dating.backend.service;

import com.dating.backend.config.JwtUtil;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.PhoneVerificationConfirmRequest;
import com.dating.backend.dto.PhoneVerificationRequest;
import com.dating.backend.dto.PhoneVerificationStartResponse;
import com.dating.backend.dto.UserResponse;
import com.dating.backend.entity.PhoneVerification;
import com.dating.backend.entity.User;
import com.dating.backend.exception.FocusableResponseStatusException;
import com.dating.backend.repository.PhoneVerificationRepository;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PhoneAuthService {

    private static final String PROVIDER_PHONE = "PHONE";
    private static final int CODE_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final int REQUEST_WINDOW_MINUTES = 10;

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AccountReviewPolicyService accountReviewPolicyService;
    private final BlockedIdentityService blockedIdentityService;
    private final RefreshTokenService refreshTokenService;
    private final PhoneSmsSender phoneSmsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.phone-auth.expose-dev-code:false}")
    private boolean exposeDevCode;

    /**
     * 전화번호 로그인 또는 자동가입용 인증번호를 발급합니다.
     */
    @Transactional
    public PhoneVerificationStartResponse requestCode(PhoneVerificationRequest request) {
        String phone = normalizePhone(request.getPhone());
        blockedIdentityService.validateSignupAllowed(null, phone);
        validateRequestRateLimit(phone);
        return issueVerificationCode(phone);
    }

    /**
     * 로그인한 사용자의 전화번호 연결용 인증번호를 발급합니다.
     */
    @Transactional
    public PhoneVerificationStartResponse requestCodeForProfile(String email, PhoneVerificationRequest request) {
        User user = getUserByEmail(email);
        String phone = normalizePhone(request.getPhone());

        blockedIdentityService.validateSignupAllowed(null, phone);
        validatePhoneOwnership(phone, user);
        validateRequestRateLimit(phone);
        return issueVerificationCode(phone);
    }

    /**
     * 전화번호 인증을 완료하고 기존 회원 로그인 또는 신규 회원 자동가입을 처리합니다.
     */
    @Transactional
    public AuthResponse verifyAndLogin(PhoneVerificationConfirmRequest request) {
        String phone = normalizePhone(request.getPhone());
        completeVerification(phone, request.getCode());

        User user = userRepository.findByPhone(phone).orElseGet(() -> createPhoneUser(phone));
        blockedIdentityService.validateLoginAllowed(user);

        if ("SUSPENDED".equals(user.getStatus()) || "DELETED".equals(user.getStatus()) || "HIGH_RISK".equals(user.getFraudReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "계정 상태로 인해 로그인할 수 없습니다.");
        }

        return new AuthResponse(
                jwtUtil.createToken(user.getEmail()),
                refreshTokenService.issueRefreshToken(user),
                UserResponse.from(user)
        );
    }

    /**
     * 로그인한 사용자의 전화번호를 인증 후 계정에 저장합니다.
     */
    @Transactional
    public MessageResponse verifyAndAttachPhone(String email, PhoneVerificationConfirmRequest request) {
        User user = getUserByEmail(email);
        String phone = normalizePhone(request.getPhone());

        validatePhoneOwnership(phone, user);
        completeVerification(phone, request.getCode());

        user.setPhone(phone);
        userRepository.save(user);

        return new MessageResponse("전화번호 인증이 완료되었습니다.");
    }

    /**
     * 인증번호 발급과 개발용 안내 메시지를 생성합니다.
     */
    private PhoneVerificationStartResponse issueVerificationCode(String phone) {
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES);

        phoneVerificationRepository.save(PhoneVerification.builder()
                .phone(phone)
                .code(code)
                .expiresAt(expiresAt)
                .build());

        phoneSmsSender.sendVerificationCode(phone, code, expiresAt);

        return new PhoneVerificationStartResponse(
                "인증번호를 발급했습니다. 현재 개발 환경에서는 서버 로그의 [DEV SMS AUTH] 항목에서 코드를 확인할 수 있습니다.",
                expiresAt,
                exposeDevCode ? code : null
        );
    }

    /**
     * 최신 인증 요청을 검증하고 성공 상태로 전환합니다.
     */
    private PhoneVerification completeVerification(String phone, String code) {
        PhoneVerification verification = phoneVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new FocusableResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "인증번호를 먼저 요청해 주세요.",
                        "phone"
                ));

        if (Boolean.TRUE.equals(verification.getVerified())) {
            throw new FocusableResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 인증이 완료된 요청입니다. 새 인증번호를 요청해 주세요.",
                    "phone"
            );
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new FocusableResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "인증번호가 만료되었습니다. 새 인증번호를 요청해 주세요.",
                    "phone"
            );
        }

        if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new FocusableResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "인증 시도 횟수를 초과했습니다. 새 인증번호를 요청해 주세요.",
                    "phone"
            );
        }

        verification.setAttemptCount(verification.getAttemptCount() + 1);
        if (!verification.getCode().equals(code)) {
            phoneVerificationRepository.save(verification);
            throw new FocusableResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "인증번호가 올바르지 않습니다. 다시 확인해 주세요.",
                    "code"
            );
        }

        verification.setVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        return phoneVerificationRepository.save(verification);
    }

    /**
     * 현재 사용자에게 연결 가능한 전화번호인지 확인합니다.
     */
    private void validatePhoneOwnership(String phone, User currentUser) {
        userRepository.findByPhone(phone)
                .filter(foundUser -> !foundUser.getId().equals(currentUser.getId()))
                .ifPresent(foundUser -> {
                    throw new FocusableResponseStatusException(
                            HttpStatus.CONFLICT,
                            "이미 다른 계정에서 사용 중인 전화번호입니다.",
                            "phone"
                    );
                });
    }

    /**
     * 전화번호 로그인 전용 신규 계정을 생성합니다.
     */
    private User createPhoneUser(String phone) {
        User user = User.builder()
                .email(buildPhoneEmail(phone))
                .nickname(buildPhoneNickname(phone))
                .phone(phone)
                .provider(PROVIDER_PHONE)
                .status("PENDING_REVIEW")
                .reviewComment("전화번호 인증이 완료되었습니다. 3일 이내에 사진 등록과 프로필 입력을 완료해 심사를 진행해 주세요.")
                .reviewDeadlineAt(accountReviewPolicyService.createSignupDeadline())
                .build();

        return userRepository.save(user);
    }

    /**
     * 현재 로그인 이메일로 사용자 정보를 찾습니다.
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));
    }

    /**
     * 전화번호를 숫자만 남기도록 정규화합니다.
     */
    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    /**
     * 짧은 시간 동안 너무 많은 인증 요청이 들어오지 않도록 제한합니다.
     */
    private void validateRequestRateLimit(String phone) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(REQUEST_WINDOW_MINUTES);
        long recentRequestCount = phoneVerificationRepository.countByPhoneAndCreatedAtAfter(phone, windowStart);
        if (recentRequestCount >= MAX_REQUESTS_PER_WINDOW) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /**
     * 전화번호 로그인 전용 임시 이메일을 생성합니다.
     */
    private String buildPhoneEmail(String phone) {
        return "phone_" + phone + "@phone.local";
    }

    /**
     * 전화번호 로그인 전용 임시 닉네임을 생성합니다.
     */
    private String buildPhoneNickname(String phone) {
        return "phone_" + phone;
    }
}
