/**
 * BlockedIdentityService 비즈니스 로직
 */
package com.dating.backend.service;

import com.dating.backend.dto.BlockedIdentityResponse;
import com.dating.backend.entity.BlockedIdentity;
import com.dating.backend.entity.FraudRiskLog;
import com.dating.backend.entity.User;
import com.dating.backend.repository.BlockedIdentityRepository;
import com.dating.backend.repository.FraudRiskLogRepository;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BlockedIdentityService {

    public static final String TYPE_EMAIL = "EMAIL";
    public static final String TYPE_PHONE = "PHONE";

    private final BlockedIdentityRepository blockedIdentityRepository;
    private final FraudRiskLogRepository fraudRiskLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void validateSignupAllowed(String email, String phone) {
        if (isBlocked(TYPE_EMAIL, normalize(email))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "차단된 이메일입니다. 가입할 수 없습니다.");
        }

        if (phone != null && !phone.isBlank() && isBlocked(TYPE_PHONE, normalize(phone))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "차단된 전화번호입니다. 가입할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public void validateLoginAllowed(User user) {
        if (isBlocked(TYPE_EMAIL, normalize(user.getEmail()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "차단된 계정입니다. 로그인할 수 없습니다.");
        }

        if (user.getPhone() != null && !user.getPhone().isBlank() && isBlocked(TYPE_PHONE, normalize(user.getPhone()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "차단된 계정입니다. 로그인할 수 없습니다.");
        }
    }

    @Transactional
    public List<String> blockUserIdentities(User user, String reason) {
        List<String> blockedTypes = new ArrayList<>();
        blockedTypes.add(blockIdentity(TYPE_EMAIL, user.getEmail(), user.getId(), reason));

        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            blockedTypes.add(blockIdentity(TYPE_PHONE, user.getPhone(), user.getId(), reason));
        }

        return blockedTypes.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    @Transactional
    public List<String> unblockUserIdentities(User user) {
        List<String> releasedTypes = new ArrayList<>();
        releasedTypes.add(unblockIdentity(TYPE_EMAIL, user.getEmail()));

        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            releasedTypes.add(unblockIdentity(TYPE_PHONE, user.getPhone()));
        }

        return releasedTypes.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveBlockedIdentity(User user) {
        return isBlocked(TYPE_EMAIL, normalize(user.getEmail()))
                || (user.getPhone() != null && !user.getPhone().isBlank() && isBlocked(TYPE_PHONE, normalize(user.getPhone())));
    }

    @Transactional(readOnly = true)
    public String summarizeBlockedTypes(User user) {
        List<String> types = new ArrayList<>();
        if (isBlocked(TYPE_EMAIL, normalize(user.getEmail()))) {
            types.add("이메일");
        }
        if (user.getPhone() != null && !user.getPhone().isBlank() && isBlocked(TYPE_PHONE, normalize(user.getPhone()))) {
            types.add("전화번호");
        }
        return String.join(", ", types);
    }

    @Transactional(readOnly = true)
    public List<BlockedIdentityResponse> getActiveBlockedIdentities(String q, String identityType) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String normalizedIdentityType = identityType == null ? "ALL" : identityType.trim().toUpperCase(Locale.ROOT);

        return blockedIdentityRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(identity -> {
                    User sourceUser = identity.getSourceUserId() == null
                            ? null
                            : userRepository.findById(identity.getSourceUserId()).orElse(null);

                    return new BlockedIdentityResponse(
                            identity.getId(),
                            identity.getIdentityType(),
                            maskIdentityValue(identity.getIdentityType(), identity.getIdentityValue()),
                            identity.getSourceUserId(),
                            sourceUser != null ? sourceUser.getEmail() : null,
                            sourceUser != null ? sourceUser.getNickname() : null,
                            identity.getReason(),
                            identity.getActive(),
                            identity.getCreatedAt(),
                            identity.getUpdatedAt()
                    );
                })
                .filter(identity -> ("ALL".equals(normalizedIdentityType) || identity.getIdentityType().equalsIgnoreCase(normalizedIdentityType))
                        && (query.isBlank()
                        || contains(identity.getIdentityType(), query)
                        || contains(identity.getIdentityValue(), query)
                        || contains(identity.getSourceUserEmail(), query)
                        || contains(identity.getSourceUserNickname(), query)
                        || contains(identity.getReason(), query)))
                .toList();
    }

    @Transactional
    public void releaseBlockedIdentity(Long blockedIdentityId) {
        BlockedIdentity identity = blockedIdentityRepository.findById(blockedIdentityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "차단된 신원 정보를 찾을 수 없습니다."));

        identity.setActive(false);
        identity.setUpdatedAt(LocalDateTime.now());
        blockedIdentityRepository.save(identity);

        if (identity.getSourceUserId() != null) {
            fraudRiskLogRepository.save(FraudRiskLog.builder()
                    .userId(identity.getSourceUserId())
                    .riskType("IDENTITY_UNBLOCKED")
                    .score(0)
                    .detail(identity.getIdentityType() + " 차단이 해제되었습니다.")
                    .build());
        }
    }

    private String blockIdentity(String type, String rawValue, Long sourceUserId, String reason) {
        String normalizedValue = normalize(rawValue);
        if (normalizedValue == null || normalizedValue.isBlank()) {
            return null;
        }

        BlockedIdentity identity = blockedIdentityRepository
                .findByIdentityTypeAndIdentityValueAndActiveTrue(type, normalizedValue)
                .orElseGet(() -> BlockedIdentity.builder()
                        .identityType(type)
                        .identityValue(normalizedValue)
                        .build());

        identity.setSourceUserId(sourceUserId);
        identity.setReason(reason == null || reason.isBlank() ? "운영자 판단으로 차단되었습니다." : reason);
        identity.setActive(true);
        identity.setUpdatedAt(LocalDateTime.now());
        blockedIdentityRepository.save(identity);

        if (sourceUserId != null) {
            fraudRiskLogRepository.save(FraudRiskLog.builder()
                    .userId(sourceUserId)
                    .riskType("IDENTITY_BLOCKED")
                    .score(100)
                    .detail(identity.getIdentityType() + " 차단: " + identity.getReason())
                    .build());
        }
        return type;
    }

    private String unblockIdentity(String type, String rawValue) {
        String normalizedValue = normalize(rawValue);
        if (normalizedValue == null || normalizedValue.isBlank()) {
            return null;
        }

        blockedIdentityRepository.findByIdentityTypeAndIdentityValueAndActiveTrue(type, normalizedValue)
                .ifPresent(identity -> {
                    identity.setActive(false);
                    identity.setUpdatedAt(LocalDateTime.now());
                    blockedIdentityRepository.save(identity);

                    if (identity.getSourceUserId() != null) {
                        fraudRiskLogRepository.save(FraudRiskLog.builder()
                                .userId(identity.getSourceUserId())
                                .riskType("IDENTITY_UNBLOCKED")
                                .score(0)
                                .detail(identity.getIdentityType() + " 차단이 해제되었습니다.")
                                .build());
                    }
                });

        return type;
    }

    private boolean isBlocked(String type, String normalizedValue) {
        if (normalizedValue == null || normalizedValue.isBlank()) {
            return false;
        }
        return blockedIdentityRepository.existsByIdentityTypeAndIdentityValueAndActiveTrue(type, normalizedValue);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String maskIdentityValue(String type, String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        if (TYPE_EMAIL.equalsIgnoreCase(type)) {
            String[] parts = value.split("@", 2);
            if (parts.length != 2) {
                return value;
            }

            String local = parts[0];
            if (local.length() <= 1) {
                return "*@" + parts[1].toLowerCase(Locale.ROOT);
            }
            if (local.length() == 2) {
                return local.charAt(0) + "*@" + parts[1].toLowerCase(Locale.ROOT);
            }
            return local.substring(0, 2) + "***@" + parts[1].toLowerCase(Locale.ROOT);
        }

        if (TYPE_PHONE.equalsIgnoreCase(type)) {
            String digits = value.replaceAll("[^0-9]", "");
            if (digits.length() < 4) {
                return "****";
            }
            return "*******" + digits.substring(digits.length() - 4);
        }

        return value;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
