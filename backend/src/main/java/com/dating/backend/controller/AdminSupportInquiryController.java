/**
 * 관리자 고객 문의 운영 API를 제공합니다.
 */
package com.dating.backend.controller;

import com.dating.backend.dto.AdminSupportInquiryResponse;
import com.dating.backend.dto.AdminSupportInquirySummaryResponse;
import com.dating.backend.dto.AdminSupportInquiryUpdateRequest;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.service.AdminReviewService;
import com.dating.backend.service.AdminSupportInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
public class AdminSupportInquiryController {

    private final AdminReviewService adminReviewService;
    private final AdminSupportInquiryService adminSupportInquiryService;

    @GetMapping("/summary")
    public AdminSupportInquirySummaryResponse getSummary(@RequestHeader("X-Admin-Key") String adminKey) {
        adminReviewService.validateAdminKey(adminKey);
        return adminSupportInquiryService.getSummary();
    }

    @GetMapping("/inquiries")
    public List<AdminSupportInquiryResponse> getInquiries(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "") String q
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminSupportInquiryService.getInquiries(status, q);
    }

    @PutMapping("/inquiries/{inquiryId}")
    public MessageResponse updateInquiry(
            @PathVariable Long inquiryId,
            @RequestHeader("X-Admin-Key") String adminKey,
            @Valid @RequestBody AdminSupportInquiryUpdateRequest request
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminSupportInquiryService.updateInquiry(inquiryId, request);
    }
}
