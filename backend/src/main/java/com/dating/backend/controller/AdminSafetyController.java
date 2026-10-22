/**
 * AdminSafetyController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.AdminRiskActionRequest;
import com.dating.backend.dto.AdminUserReportResponse;
import com.dating.backend.dto.BlockedIdentityResponse;
import com.dating.backend.dto.FraudRiskLogResponse;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.ResolveReportRequest;
import com.dating.backend.dto.ScamMonitorSummaryResponse;
import com.dating.backend.dto.ScamMonitorUserResponse;
import com.dating.backend.service.BlockedIdentityService;
import com.dating.backend.service.SafetyService;
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
@RequestMapping("/api/admin/safety")
@RequiredArgsConstructor
public class AdminSafetyController {

    private final SafetyService safetyService;
    private final BlockedIdentityService blockedIdentityService;

    @GetMapping("/summary")
    public ScamMonitorSummaryResponse getSummary(@RequestHeader("X-Admin-Key") String adminKey) {
        safetyService.validateAdminKey(adminKey);
        return safetyService.getSummary();
    }

    @GetMapping("/users")
    public List<ScamMonitorUserResponse> getRiskUsers(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam(defaultValue = "WATCH") String riskStatus,
            @RequestParam(defaultValue = "") String q
    ) {
        safetyService.validateAdminKey(adminKey);
        return safetyService.getRiskUsers(riskStatus, q);
    }

    @GetMapping("/users/{userId}/logs")
    public List<FraudRiskLogResponse> getRiskLogs(
            @RequestHeader("X-Admin-Key") String adminKey,
            @PathVariable Long userId
    ) {
        safetyService.validateAdminKey(adminKey);
        return safetyService.getRiskLogs(userId);
    }

    @PutMapping("/users/{userId}/review")
    public MessageResponse reviewRiskUser(
            @RequestHeader("X-Admin-Key") String adminKey,
            @PathVariable Long userId,
            @Valid @RequestBody AdminRiskActionRequest request
    ) {
        safetyService.validateAdminKey(adminKey);
        return safetyService.reviewRiskUser(userId, request);
    }

    @GetMapping("/reports")
    public List<AdminUserReportResponse> getReports(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam(defaultValue = "OPEN") String status
    ) {
        safetyService.validateAdminKey(adminKey);
        return safetyService.getReports(status);
    }

    @GetMapping("/blocked-identities")
    public List<BlockedIdentityResponse> getBlockedIdentities(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ALL") String identityType
    ) {
        safetyService.validateAdminKey(adminKey);
        return blockedIdentityService.getActiveBlockedIdentities(q, identityType);
    }

    @PutMapping("/blocked-identities/{blockedIdentityId}/release")
    public MessageResponse releaseBlockedIdentity(
            @RequestHeader("X-Admin-Key") String adminKey,
            @PathVariable Long blockedIdentityId
    ) {
        safetyService.validateAdminKey(adminKey);
        blockedIdentityService.releaseBlockedIdentity(blockedIdentityId);
        return new MessageResponse("차단된 신원 정보가 해제되었습니다.");
    }

    @PutMapping("/reports/{reportId}/resolve")
    public MessageResponse resolveReport(
            @RequestHeader("X-Admin-Key") String adminKey,
            @PathVariable Long reportId,
            @Valid @RequestBody ResolveReportRequest request
    ) {
        safetyService.validateAdminKey(adminKey);
        return safetyService.resolveReport(reportId, request);
    }
}
