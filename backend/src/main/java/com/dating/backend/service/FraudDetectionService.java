/**
 * FraudDetectionService 비즈니스 로직
 */
package com.dating.backend.service;

import com.dating.backend.entity.FraudRiskLog;
import com.dating.backend.entity.User;
import com.dating.backend.entity.UserProfile;
import com.dating.backend.repository.FraudRiskLogRepository;
import com.dating.backend.repository.UserProfileRepository;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FraudRiskLogRepository fraudRiskLogRepository;
    private final com.dating.backend.matching.SafetyEventService safetyEvents;

    @Transactional
    public void evaluateUserProfile(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        if (user == null || profile == null) {
            return;
        }

        String text = String.join(" ",
                safe(profile.getJob()),
                safe(profile.getPersonality()),
                safe(profile.getIdealType()),
                safe(profile.getIntroduction())
        ).toLowerCase(Locale.ROOT);

        Map<String, Integer> rules = new LinkedHashMap<>();
        rules.put("급전", 35);
        rules.put("투자", 35);
        rules.put("송금", 30);
        rules.put("코인", 35);
        rules.put("비트코인", 40);
        rules.put("선입금", 30);
        rules.put("환전", 35);
        rules.put("대출", 30);
        rules.put("고수익", 30);
        rules.put("해외", 25);
        rules.put("출금", 20);
        rules.put("수수료", 20);
        rules.put("링크", 20);
        rules.put("계좌", 20);

        List<String> matchedKeywords = new ArrayList<>();
        int score = 0;

        for (Map.Entry<String, Integer> entry : rules.entrySet()) {
            if (text.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                matchedKeywords.add(entry.getKey());
                score += entry.getValue();
            }
        }

        score = Math.min(score, 100);

        String nextStatus = score >= 60 ? "HIGH_RISK" : score >= 30 ? "WATCH" : "NORMAL";
        boolean changed = !nextStatus.equals(user.getFraudReviewStatus()) || !Integer.valueOf(score).equals(user.getFraudRiskScore());

        user.setFraudRiskScore(score);
        user.setFraudReviewStatus(nextStatus);

        // Keyword matches are review signals, never proof of fraud or an automatic suspension.

        userRepository.save(user);

        if (changed && score >= 30) {
            String detail = matchedKeywords.isEmpty()
                    ? "스캠 의심 신호가 감지되었지만 구체 키워드는 없습니다."
                    : "스캠 의심 키워드: " + String.join(", ", matchedKeywords);

            safetyEvents.record(userId,null,null,"PROFILE_KEYWORD",detail,score);
            fraudRiskLogRepository.save(FraudRiskLog.builder()
                    .userId(userId)
                    .riskType("PROFILE_KEYWORD")
                    .score(score)
                    .detail(detail)
                    .build());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
