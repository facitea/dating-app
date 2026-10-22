/**
 * refresh token 발급, 검증, access token 재발급을 담당하는 서비스
 */
package com.dating.backend.service;

import com.dating.backend.config.JwtUtil;
import com.dating.backend.dto.AuthResponse;
import com.dating.backend.dto.UserResponse;
import com.dating.backend.entity.RefreshToken;
import com.dating.backend.entity.User;
import com.dating.backend.repository.RefreshTokenRepository;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_DAYS = 14;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BlockedIdentityService blockedIdentityService;

    @Transactional
    public String issueRefreshToken(User user) {
        String rawToken = createRawToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS))
                .build());
        return rawToken;
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(rawRefreshToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 유지 토큰이 유효하지 않습니다."));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            revoke(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 유지 시간이 만료되었습니다. 다시 로그인해 주세요.");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        validateRefreshAllowed(user);

        revoke(refreshToken);
        String newRefreshToken = issueRefreshToken(user);
        return new AuthResponse(jwtUtil.createToken(user.getEmail()), newRefreshToken, UserResponse.from(user));
    }

    private void validateRefreshAllowed(User user) {
        blockedIdentityService.validateLoginAllowed(user);
        if ("SUSPENDED".equals(user.getStatus()) || "DELETED".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "현재 계정 상태에서는 로그인 유지가 불가능합니다.");
        }
        if ("HIGH_RISK".equals(user.getFraudReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "스캠 의심 계정으로 분류되어 로그인할 수 없습니다.");
        }
    }

    private void revoke(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    private String createRawToken() {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
