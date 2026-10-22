/**
 * PhoneVerificationRepository JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    Optional<PhoneVerification> findTopByPhoneOrderByCreatedAtDesc(String phone);

    List<PhoneVerification> findTop50ByOrderByCreatedAtDesc();

    long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime createdAt);
}
