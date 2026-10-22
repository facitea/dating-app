/**
 * SafetyController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.UserReportRequest;
import com.dating.backend.dto.UserReportResponse;
import com.dating.backend.service.SafetyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    @PostMapping("/reports")
    public UserReportResponse reportUser(
            Authentication authentication,
            @Valid @RequestBody UserReportRequest request
    ) {
        return safetyService.reportUser(authentication.getName(), request);
    }
}