/**
 * 자체 인증번호 이력을 조회하는 JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.AuthVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthVerificationRepository extends JpaRepository<AuthVerification, Long> {

    Optional<AuthVerification> findTopByTargetTypeAndTargetValueAndPurposeOrderByCreatedAtDesc(
            String targetType,
            String targetValue,
            String purpose
    );

    Optional<AuthVerification> findByTargetTypeAndTargetValueAndPurposeAndVerificationTokenAndVerifiedTrue(
            String targetType,
            String targetValue,
            String purpose,
            String verificationToken
    );
}
