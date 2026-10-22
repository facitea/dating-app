/**
 * 관리자 고객 문의 운영 기능을 처리합니다.
 */
package com.dating.backend.service;

import com.dating.backend.dto.AdminSupportInquiryResponse;
import com.dating.backend.dto.AdminSupportInquirySummaryResponse;
import com.dating.backend.dto.AdminSupportInquiryUpdateRequest;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.entity.SupportInquiry;
import com.dating.backend.repository.SupportInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminSupportInquiryService {

    private final com.dating.backend.matching.InboxService inbox;
    private final SupportInquiryRepository supportInquiryRepository;

    /**
     * 고객 문의 상태별 요약을 반환합니다.
     */
    @Transactional(readOnly = true)
    public AdminSupportInquirySummaryResponse getSummary() {
        return new AdminSupportInquirySummaryResponse(
                supportInquiryRepository.countByStatus("OPEN"),
                supportInquiryRepository.countByStatus("IN_PROGRESS"),
                supportInquiryRepository.countByStatus("RESOLVED"),
                supportInquiryRepository.countByStatus("CLOSED")
        );
    }

    /**
     * 상태와 검색어 기준으로 고객 문의 목록을 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<AdminSupportInquiryResponse> getInquiries(String status, String query) {
        String normalizedStatus = status == null || status.isBlank() ? "OPEN" : status.trim().toUpperCase(Locale.ROOT);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<SupportInquiry> inquiries = "ALL".equals(normalizedStatus)
                ? supportInquiryRepository.findAllByOrderByCreatedAtDesc()
                : supportInquiryRepository.findByStatusOrderByCreatedAtDesc(normalizedStatus);

        return inquiries.stream()
                .filter(inquiry -> normalizedQuery.isBlank() || matchesQuery(inquiry, normalizedQuery))
                .map(AdminSupportInquiryResponse::from)
                .toList();
    }

    /**
     * 운영자 답변과 처리 상태를 저장합니다.
     */
    @Transactional
    public MessageResponse updateInquiry(Long inquiryId, AdminSupportInquiryUpdateRequest request) {
        SupportInquiry inquiry = supportInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의 내역을 찾을 수 없습니다."));

        if(!List.of("OPEN","IN_PROGRESS","RESOLVED","CLOSED").contains(request.getStatus().trim().toUpperCase(Locale.ROOT)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"올바른 문의 상태를 선택해 주세요.");
        inquiry.setStatus(request.getStatus().trim().toUpperCase(Locale.ROOT));
        inquiry.setAdminReply(request.getAdminReply() == null || request.getAdminReply().isBlank() ? null : request.getAdminReply().trim());
        inquiry.setUpdatedAt(LocalDateTime.now());
        supportInquiryRepository.save(inquiry);
        if(inquiry.getAdminReply()!=null&&inquiry.getUserId()!=null)inbox.add(inquiry.getUserId(),"SUPPORT:"+inquiry.getId()+":"+System.currentTimeMillis(),"고객센터 답변이 도착했어요","문의 내역에서 답변을 확인해 주세요.","/support");

        return new MessageResponse("문의 처리 상태를 저장했습니다.");
    }

    private boolean matchesQuery(SupportInquiry inquiry, String query) {
        return contains(inquiry.getEmail(), query)
                || contains(inquiry.getNickname(), query)
                || contains(inquiry.getCategory(), query)
                || contains(inquiry.getQuestion(), query)
                || contains(inquiry.getAdminReply(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
