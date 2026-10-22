/**
 * AdminReviewHistoryRepository JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.AdminReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminReviewHistoryRepository extends JpaRepository<AdminReviewHistory, Long> {

    List<AdminReviewHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}