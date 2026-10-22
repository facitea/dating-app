/**
 * UserVerificationRepository JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.UserVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserVerificationRepository extends JpaRepository<UserVerification, Long> {
    Optional<UserVerification> findByUserId(Long userId);
}