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

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update User u set u.lastActiveAt=:now where u.id=:id and (u.lastActiveAt is null or u.lastActiveAt<:cutoff)")
    void touchActivity(@org.springframework.data.repository.query.Param("id") Long id,@org.springframework.data.repository.query.Param("now") java.time.Instant now,@org.springframework.data.repository.query.Param("cutoff") java.time.Instant cutoff);
    Optional<User> findByFirebaseUid(String firebaseUid);

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