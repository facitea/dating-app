/**
 * FraudRiskLogRepository JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.FraudRiskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudRiskLogRepository extends JpaRepository<FraudRiskLog, Long> {

    List<FraudRiskLog> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}