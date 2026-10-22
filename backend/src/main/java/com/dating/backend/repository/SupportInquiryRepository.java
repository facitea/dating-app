/**
 * 고객센터 문의 저장소입니다.
 */
package com.dating.backend.repository;

import com.dating.backend.entity.SupportInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportInquiryRepository extends JpaRepository<SupportInquiry, Long> {

    List<SupportInquiry> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    List<SupportInquiry> findAllByOrderByCreatedAtDesc();

    List<SupportInquiry> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
