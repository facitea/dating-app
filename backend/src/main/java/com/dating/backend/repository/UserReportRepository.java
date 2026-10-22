/**
 * UserReportRepository JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    long countByStatus(String status);

    long countByReportedUserIdAndStatus(Long reportedUserId, String status);

    boolean existsByReporterUserIdAndReportedUserIdAndStatus(Long reporterUserId, Long reportedUserId, String status);

    List<UserReport> findByStatusOrderByCreatedAtDesc(String status);
}