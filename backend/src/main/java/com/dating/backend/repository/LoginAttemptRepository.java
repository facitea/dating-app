/**
 * 로그인 실패 이력을 조회하는 JPA 리포지토리
 */
package com.dating.backend.repository;

import com.dating.backend.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    Optional<LoginAttempt> findByLoginId(String loginId);
}
