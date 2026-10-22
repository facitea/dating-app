/**
 * AdminPhoneAuthController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.AdminPhoneVerificationLogResponse;
import com.dating.backend.repository.PhoneVerificationRepository;
import com.dating.backend.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/phone-auth")
@RequiredArgsConstructor
public class AdminPhoneAuthController {

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final AdminReviewService adminReviewService;

    @GetMapping("/logs")
    public List<AdminPhoneVerificationLogResponse> getPhoneAuthLogs(@RequestHeader("X-Admin-Key") String adminKey) {
        adminReviewService.validateAdminKey(adminKey);
        return phoneVerificationRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(AdminPhoneVerificationLogResponse::from)
                .toList();
    }
}