/**
 * UserRepository JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    long countByStatus(String status);

    long countByStatusInAndReviewDeadlineAtBefore(Collection<String> statuses, LocalDateTime reviewDeadlineAt);

    long countByFraudReviewStatus(String fraudReviewStatus);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByResetPasswordToken(String resetPasswordToken);

    List<User> findByStatusOrderByCreatedAtAsc(String status);

    List<User> findByStatusInAndReviewDeadlineAtBefore(Collection<String> statuses, LocalDateTime reviewDeadlineAt);

    List<User> findByFraudReviewStatusInOrderByFraudRiskScoreDescCreatedAtDesc(Collection<String> fraudReviewStatuses);
}