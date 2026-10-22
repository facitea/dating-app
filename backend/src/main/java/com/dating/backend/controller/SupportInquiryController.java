/**
 * 고객센터 문의 API를 제공합니다.
 */
package com.dating.backend.controller;

import com.dating.backend.dto.SupportInquiryCreateRequest;
import com.dating.backend.dto.SupportInquiryResponse;
import com.dating.backend.service.SupportInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support/inquiries")
@RequiredArgsConstructor
public class SupportInquiryController {

    private final SupportInquiryService supportInquiryService;

    @PostMapping
    public SupportInquiryResponse createInquiry(
            Authentication authentication,
            @Valid @RequestBody SupportInquiryCreateRequest request
    ) {
        return supportInquiryService.createInquiry(authentication.getName(), request);
    }

    @GetMapping("/me")
    public List<SupportInquiryResponse> getMyInquiries(Authentication authentication) {
        return supportInquiryService.getMyInquiries(authentication.getName());
    }
}
