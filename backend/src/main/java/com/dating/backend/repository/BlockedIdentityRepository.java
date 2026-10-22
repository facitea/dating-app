/**
 * BlockedIdentityRepository JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.BlockedIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedIdentityRepository extends JpaRepository<BlockedIdentity, Long> {

    boolean existsByIdentityTypeAndIdentityValueAndActiveTrue(String identityType, String identityValue);

    Optional<BlockedIdentity> findByIdentityTypeAndIdentityValueAndActiveTrue(String identityType, String identityValue);

    List<BlockedIdentity> findBySourceUserIdAndActiveTrueOrderByCreatedAtDesc(Long sourceUserId);

    List<BlockedIdentity> findByActiveTrueOrderByCreatedAtDesc();
}