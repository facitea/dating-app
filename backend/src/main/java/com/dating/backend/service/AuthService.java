/**
 * AuthService 비즈니스 로직
 */
package com.dating.backend.service;

import com.dating.backend.config.JwtUtil;
import com.dating.backend.dto.AuthRequest;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.ForgotPasswordRequest;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.PasswordResetConfirmRequest;
import com.dating.backend.dto.PasswordResetRequestResponse;
import com.dating.backend.dto.SignupRequest;
import com.dating.backend.dto.UserResponse;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.entity.UserVerification;
import com.dating.backend.exception.FocusableResponseStatusException;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserRepository;
import com.dating.backend.repository.UserVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String PROVIDER_LOCAL = "LOCAL";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AccountReviewPolicyService accountReviewPolicyService;
    private final FraudDetectionService fraudDetectionService;
    private final BlockedIdentityService blockedIdentityService;
    private final AuthVerificationService authVerificationService;
    private final LoginAttemptService loginAttemptService;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    private final RefreshTokenService refreshTokenService;
    private final com.dating.backend.matching.SanctionService sanctions;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if(!request.isAgreedToTerms()||!request.isAgreedToPrivacy()||!request.isAdultConfirmed()||!"2026-10-21-draft".equals(request.getTermsVersion()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"필수 약관과 최신 버전 동의를 확인해 주세요.");
        validateAdultSignup(request);
        authVerificationService.validateSignupEmailVerified(request.getEmail(), request.getEmailVerificationToken());
        blockedIdentityService.validateSignupAllowed(request.getEmail(), null);

        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingUser == null && userRepository.existsByNickname(request.getNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        if (existingUser != null
                && !request.getNickname().equals(existingUser.getNickname())
                && userRepository.existsByNickname(request.getNickname())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        if (existingUser != null) {
            if("SUSPENDED".equals(existingUser.getStatus())||existingUser.getSanctionType()!=null)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"정지된 계정은 다시 가입할 수 없습니다.");
            if (hasLocalPassword(existingUser)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "이미 가입된 이메일입니다. 다른 이메일을 사용해 주세요."
                );
            }

            existingUser.setNickname(request.getNickname());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setProvider(PROVIDER_LOCAL);
            existingUser.setStatus("PENDING_REVIEW");
            existingUser.setReviewComment("회원 가입 심사 대기 중입니다. 3일 내에 사진 등록과 프로필 입력을 완료해 주세요.");
            existingUser.setReviewDeadlineAt(accountReviewPolicyService.createSignupDeadline());
            existingUser.setDeletedAt(null);
            clearResetToken(existingUser);

            User linkedUser = userRepository.save(existingUser);
            upsertProfile(linkedUser, request);
            upsertVerification(linkedUser, request);
            fraudDetectionService.evaluateUserProfile(linkedUser.getId());
            consent(linkedUser,request);return buildAuthResponse(linkedUser);
        }

        User user = User.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .provider(PROVIDER_LOCAL)
                .status("PENDING_REVIEW")
                .reviewComment("회원 가입 심사 대기 중입니다. 3일 내에 사진 등록과 프로필 입력을 완료해 주세요.")
                .reviewDeadlineAt(accountReviewPolicyService.createSignupDeadline())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        upsertProfile(savedUser, request);
        upsertVerification(savedUser, request);
        fraudDetectionService.evaluateUserProfile(savedUser.getId());
        consent(savedUser,request);return buildAuthResponse(savedUser);
    }

    private void consent(User u,SignupRequest r){var c=new com.dating.backend.matching.TermsConsent();c.setUserId(u.getId());c.setVersion(r.getTermsVersion());c.setTerms(r.isAgreedToTerms());c.setPrivacy(r.isAgreedToPrivacy());c.setAdult(r.isAdultConfirmed());c.setMarketing(r.isMarketingConsent());em.persist(c);}
    @Transactional
    public AuthResponse login(AuthRequest request) {
        loginAttemptService.validateNotLocked(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            loginAttemptService.recordFailure(request.getEmail());
            throw signupRequiredException();
        }

        sanctions.expire(user.getId());
        blockedIdentityService.validateLoginAllowed(user);

        if ("SUSPENDED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PERMANENT".equals(user.getSanctionType())?"영구 정지된 계정입니다. 고객센터에 문의해 주세요.":user.getSuspendedUntil()!=null?"기간 정지 중입니다. 종료: "+user.getSuspendedUntil().atZone(java.time.ZoneId.of("Asia/Seoul")):"정지된 계정입니다. 고객센터에 문의해 주세요.");
        }

        if ("DELETED".equals(user.getStatus())) {
            throw new FocusableResponseStatusException(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다. 다시 가입해 주세요.", "email");
        }

        if ("HIGH_RISK".equals(user.getFraudReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "스캠 의심 계정으로 분류되어 로그인할 수 없습니다.");
        }

        if (!hasLocalPassword(user)) {
            throw new FocusableResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이 계정은 이메일 비밀번호 로그인 계정이 아닙니다. 가입했던 방식으로 로그인해 주세요.",
                    "email"
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(request.getEmail());
            throw invalidPasswordException();
        }

        loginAttemptService.recordSuccess(request.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new FocusableResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회원가입 이력이 없는 이메일입니다. 먼저 가입을 진행해 주세요.",
                        "email"
                ));

        if (!hasLocalPassword(user)) {
            throw new FocusableResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이 계정은 이메일 비밀번호 계정이 아니라 비밀번호 재설정을 진행할 수 없습니다.",
                    "email"
            );
        }

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        return new PasswordResetRequestResponse(
                "비밀번호 재설정 토큰을 발급했습니다. 30분 내에 변경을 완료해 주세요.",
                token
        );
    }

    @Transactional
    public MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 비밀번호 재설정 요청입니다."));

        if (user.getResetPasswordTokenExpiresAt() == null
                || user.getResetPasswordTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호 재설정 링크가 만료되었습니다. 다시 요청해 주세요.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setProvider(PROVIDER_LOCAL);
        clearResetToken(user);
        userRepository.save(user);

        return new MessageResponse("비밀번호가 정상적으로 변경되었습니다. 새 비밀번호로 로그인해 주세요.");
    }

    private void upsertProfile(User user, SignupRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().userId(user.getId()).build());

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

        UserProfile savedProfile = userProfileRepository.save(profile);
        updateProfileCompletedAt(user, savedProfile,
                userVerificationRepository.findByUserId(user.getId()).orElse(null));
    }

    private void upsertVerification(User user, SignupRequest request) {
        UserVerification verification = userVerificationRepository.findByUserId(user.getId())
                .orElseGet(() -> UserVerification.builder().userId(user.getId()).build());

        verification.setBirthDate(request.getBirthDate());
        verification.setGender(request.getGender());
        verification.setIsVerified(false);
        verification.setVerifiedAt(null);
        verification.setUpdatedAt(LocalDateTime.now());

        UserVerification savedVerification = userVerificationRepository.save(verification);
        updateProfileCompletedAt(user,
                userProfileRepository.findByUserId(user.getId()).orElse(null),
                savedVerification);
    }

    private void updateProfileCompletedAt(User user, UserProfile profile, UserVerification verification) {
        if (profile == null || verification == null) {
            return;
        }

        if (accountReviewPolicyService.isProfileComplete(profile, verification)) {
            if (user.getProfileCompletedAt() == null) {
                user.setProfileCompletedAt(LocalDateTime.now());
            }
        } else {
            user.setProfileCompletedAt(null);
        }
        userRepository.save(user);
    }

    private void validateAdultSignup(SignupRequest request) {
        LocalDate adultThreshold = LocalDate.now().minusYears(19);
        if (request.getBirthDate().isAfter(adultThreshold)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "만 19세 미만은 가입할 수 없습니다.");
        }
    }

    private boolean hasLocalPassword(User user) {
        return user.getPassword() != null && !user.getPassword().isBlank();
    }

    private ResponseStatusException signupRequiredException() {
        return new FocusableResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "회원가입이 필요합니다. 가입 후 로그인해 주세요.",
                "email"
        );
    }

    private ResponseStatusException invalidPasswordException() {
        return new FocusableResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "비밀번호가 올바르지 않습니다. 다시 확인해 주세요.",
                "password"
        );
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                jwtUtil.createToken(user.getEmail()),
                refreshTokenService.issueRefreshToken(user),
                UserResponse.from(user)
        );
    }

    private void clearResetToken(User user) {
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiresAt(null);
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
