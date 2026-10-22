/**
 * AdminReviewController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.AdminMemoRequest;
import com.dating.backend.dto.AdminRejectRequest;
import com.dating.backend.dto.AdminReviewCandidateResponse;
import com.dating.backend.dto.AdminReviewDecisionResponse;
import com.dating.backend.dto.AdminReviewHistoryResponse;
import com.dating.backend.dto.AdminReviewSummaryResponse;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.service.AdminReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping("/summary")
    public AdminReviewSummaryResponse getSummary(@RequestHeader("X-Admin-Key") String adminKey) {
        adminReviewService.validateAdminKey(adminKey);
        return adminReviewService.getSummary();
    }

    @GetMapping
    public List<AdminReviewCandidateResponse> getCandidates(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam(defaultValue = "PENDING_REVIEW") String status,
            @RequestParam(defaultValue = "false") boolean dueSoonOnly,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ALL") String gender,
            @RequestParam(defaultValue = "false") boolean profileCompleteOnly,
            @RequestParam(defaultValue = "LATEST") String period
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminReviewService.getCandidates(status, dueSoonOnly, q, gender, profileCompleteOnly, period);
    }

    @GetMapping("/{userId}/history")
    public List<AdminReviewHistoryResponse> getHistories(
            @PathVariable Long userId,
            @RequestHeader("X-Admin-Key") String adminKey
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminReviewService.getHistories(userId);
    }

    @PutMapping("/{userId}/memo")
    public MessageResponse updateMemo(
            @PathVariable Long userId,
            @RequestHeader("X-Admin-Key") String adminKey,
            @Valid @RequestBody AdminMemoRequest request
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminReviewService.updateMemo(userId, request);
    }

    @PostMapping("/{userId}/approve")
    public AdminReviewDecisionResponse approve(
            @PathVariable Long userId,
            @RequestHeader("X-Admin-Key") String adminKey
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminReviewService.approve(userId);
    }

    @PostMapping("/{userId}/reject")
    public AdminReviewDecisionResponse reject(
            @PathVariable Long userId,
            @RequestHeader("X-Admin-Key") String adminKey,
            @Valid @RequestBody AdminRejectRequest request
    ) {
        adminReviewService.validateAdminKey(adminKey);
        return adminReviewService.reject(userId, request);
    }
}
