/**
 * AccountCleanupScheduler 비즈니스 로직
 */
package com.dating.backend.service;

import com.dating.backend.entity.User;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCleanupScheduler {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredReviewAccounts() {
        List<User> expiredUsers = userRepository.findByStatusInAndReviewDeadlineAtBefore(
                List.of("PENDING_REVIEW", "REJECTED"),
                LocalDateTime.now()
        );

        if (expiredUsers.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        expiredUsers.forEach(user -> {
            user.setStatus("DELETED");
            user.setDeletedAt(now);
            user.setReviewDeadlineAt(null);
            user.setReviewComment("심사 기한 내에 프로필과 사진 등록을 완료하지 않아 계정이 삭제되었습니다.");
        });

        userRepository.saveAll(expiredUsers);
        log.info("심사 기한 초과로 계정 삭제 처리: {}건", expiredUsers.size());
    }
}
