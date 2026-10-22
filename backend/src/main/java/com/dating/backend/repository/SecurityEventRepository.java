/**
 * 보안 이벤트 저장소 JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
}
