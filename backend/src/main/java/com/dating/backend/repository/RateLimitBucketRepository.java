/**
 * 요청 제한 버킷 저장소 JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.RateLimitBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucket, Long> {

    Optional<RateLimitBucket> findByActionTypeAndIdentifierTypeAndIdentifierValue(
            String actionType,
            String identifierType,
            String identifierValue
    );
}
