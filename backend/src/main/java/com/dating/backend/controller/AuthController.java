/**
 * AuthController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.AuthRequest;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.AuthVerificationConfirmRequest;
import com.dating.backend.dto.AuthVerificationConfirmResponse;
import com.dating.backend.dto.AuthVerificationRequest;
import com.dating.backend.dto.AuthVerificationStartResponse;
import com.dating.backend.dto.ForgotPasswordRequest;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.PasswordResetConfirmRequest;
import com.dating.backend.dto.PasswordResetRequestResponse;
import com.dating.backend.dto.PhoneVerificationConfirmRequest;
import com.dating.backend.dto.PhoneVerificationRequest;
import com.dating.backend.dto.PhoneVerificationStartResponse;
import com.dating.backend.dto.SignupRequest;
import com.dating.backend.service.AbuseProtectionService;
import com.dating.backend.service.AbuseRequestContext;
import com.dating.backend.service.AuthService;
import com.dating.backend.service.AuthVerificationService;
import com.dating.backend.service.PhoneAuthService;
import com.dating.backend.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final PhoneAuthService phoneAuthService;
    private final AuthVerificationService authVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final AbuseProtectionService abuseProtectionService;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie-max-age-seconds:1209600}")
    private int refreshCookieMaxAgeSeconds;

    @PostMapping("/signup")
    public AuthResponse signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AbuseRequestContext context = buildAbuseContext(httpRequest);
        abuseProtectionService.protectSignup(request.getEmail(), request.getWebsite(), request.getFormStartedAt(), context);
        try {
            return withRefreshCookie(authService.signup(request), response);
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            abuseProtectionService.recordFailure("SIGNUP_FAILED", "EMAIL", request.getEmail(), context, exception.getReason());
            throw exception;
        }
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AbuseRequestContext context = buildAbuseContext(httpRequest);
        abuseProtectionService.protectLogin(request.getEmail(), context);
        try {
            return withRefreshCookie(authService.login(request), response);
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            abuseProtectionService.recordFailure("LOGIN_FAILED", "EMAIL", request.getEmail(), context, exception.getReason());
            throw exception;
        }
    }

    @PostMapping("/phone/request")
    public PhoneVerificationStartResponse requestPhoneVerification(
            @Valid @RequestBody PhoneVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        abuseProtectionService.protectPhoneVerification(request.getPhone(), buildAbuseContext(httpRequest));
        return phoneAuthService.requestCode(request);
    }

    @PostMapping("/phone/verify")
    public AuthResponse verifyPhoneAndLogin(
            @Valid @RequestBody PhoneVerificationConfirmRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AbuseRequestContext context = buildAbuseContext(httpRequest);
        try {
            return withRefreshCookie(phoneAuthService.verifyAndLogin(request), response);
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            abuseProtectionService.recordFailure("PHONE_VERIFY_FAILED", "PHONE", request.getPhone(), context, exception.getReason());
            throw exception;
        }
    }

    @PostMapping("/verification/request")
    public AuthVerificationStartResponse requestVerification(
            @Valid @RequestBody AuthVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        if ("EMAIL".equalsIgnoreCase(request.getTargetType())) {
            abuseProtectionService.protectEmailVerification(request.getTargetValue(), buildAbuseContext(httpRequest));
        }
        return authVerificationService.requestCode(request);
    }

    @PostMapping("/verification/confirm")
    public AuthVerificationConfirmResponse confirmVerification(
            @Valid @RequestBody AuthVerificationConfirmRequest request,
            HttpServletRequest httpRequest
    ) {
        AbuseRequestContext context = buildAbuseContext(httpRequest);
        try {
            return authVerificationService.confirmCode(request);
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            abuseProtectionService.recordFailure("VERIFICATION_CONFIRM_FAILED", request.getTargetType(), request.getTargetValue(), context, exception.getReason());
            throw exception;
        }
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenCookie(request);
        return withRefreshCookie(refreshTokenService.refresh(refreshToken), response);
    }

    @PostMapping("/password/reset/request")
    public PasswordResetRequestResponse requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        abuseProtectionService.protectPasswordReset(request.getEmail(), buildAbuseContext(httpRequest));
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password/reset/confirm")
    public MessageResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return authService.confirmPasswordReset(request);
    }

    private AuthResponse withRefreshCookie(AuthResponse authResponse, HttpServletResponse response) {
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return authResponse;
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(refreshCookieSecure);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(refreshCookieMaxAgeSeconds);
        cookie.setAttribute("SameSite", refreshCookieSecure ? "None" : "Lax");
        response.addCookie(cookie);
    }

    private String extractRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 유지 토큰이 없습니다.");
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 유지 토큰이 없습니다.");
    }

    private AbuseRequestContext buildAbuseContext(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ipAddress = forwardedFor != null && !forwardedFor.isBlank()
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        return new AbuseRequestContext(ipAddress, userAgent);
    }
}
