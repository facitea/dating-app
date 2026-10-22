/**
 * 인증/보안 이벤트를 기록하는 서비스
 */
package com.dating.backend.service;

import com.dating.backend.entity.SecurityEvent;
import com.dating.backend.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;

    // 인증 남용, 차단, 실패 같은 보안 이벤트를 별도 트랜잭션으로 남긴다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String eventType,
            String identifierType,
            String identifierValue,
            AbuseRequestContext context,
            String reason,
            int riskScore
    ) {
        securityEventRepository.save(SecurityEvent.builder()
                .eventType(eventType)
                .identifierType(identifierType)
                .identifierValue(maskIfBlank(identifierValue))
                .ipAddress(context.ipAddress())
                .userAgent(truncate(context.userAgent(), 500))
                .reason(truncate(reason, 500))
                .riskScore(riskScore)
                .build());
    }

    private String maskIfBlank(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : truncate(value, 255);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
