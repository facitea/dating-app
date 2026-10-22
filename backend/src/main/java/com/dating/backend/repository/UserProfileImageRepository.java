/**
 * UserProfileImageRepository JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.UserProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long> {
    List<UserProfileImage> findByUserIdOrderByImageOrderAsc(Long userId);
    long countByUserId(Long userId);
    Optional<UserProfileImage> findByUserIdAndImageOrder(Long userId, Integer imageOrder);
}