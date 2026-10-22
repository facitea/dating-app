/**
 * ProfileController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.MyProfileResponse;
import com.dating.backend.dto.MessageResponse;
import com.dating.backend.dto.PhoneVerificationConfirmRequest;
import com.dating.backend.dto.PhoneVerificationRequest;
import com.dating.backend.dto.PhoneVerificationStartResponse;
import com.dating.backend.dto.UpdateMyProfileRequest;
import com.dating.backend.service.PhoneAuthService;
import com.dating.backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final PhoneAuthService phoneAuthService;

    @GetMapping("/me")
    public MyProfileResponse getMyProfile(Authentication authentication) {
        return profileService.getMyProfile(authentication.getName());
    }

    @PutMapping("/me")
    public MyProfileResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        return profileService.updateMyProfile(authentication.getName(), request);
    }

    @PostMapping("/phone/request")
    public PhoneVerificationStartResponse requestMyPhoneVerification(
            Authentication authentication,
            @Valid @RequestBody PhoneVerificationRequest request
    ) {
        return phoneAuthService.requestCodeForProfile(authentication.getName(), request);
    }

    @PostMapping("/phone/verify")
    public MessageResponse verifyMyPhone(
            Authentication authentication,
            @Valid @RequestBody PhoneVerificationConfirmRequest request
    ) {
        return phoneAuthService.verifyAndAttachPhone(authentication.getName(), request);
    }
}
