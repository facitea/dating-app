/**
 * ProfileImageController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.ReviewStatusResponse;
import com.dating.backend.dto.UserProfileImageRequest;
import com.dating.backend.dto.UserProfileImageResponse;
import com.dating.backend.service.FileStorageService;
import com.dating.backend.service.ProfileImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profile-images")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;
    private final FileStorageService fileStorageService;

    @org.springframework.web.bind.annotation.DeleteMapping("/me/{id}") public List<UserProfileImageResponse> delete(Authentication a,@org.springframework.web.bind.annotation.PathVariable Long id){return profileImageService.delete(a.getName(),id);}
    @org.springframework.web.bind.annotation.PutMapping("/me/{id}/main") public List<UserProfileImageResponse> main(Authentication a,@org.springframework.web.bind.annotation.PathVariable Long id){return profileImageService.main(a.getName(),id);}
    @GetMapping("/me")
    public List<UserProfileImageResponse> getMyImages(Authentication authentication) {
        return profileImageService.getMyImages(authentication.getName());
    }

    @PostMapping("/me")
    public List<UserProfileImageResponse> saveMyImage(
            Authentication authentication,
            @Valid @RequestBody UserProfileImageRequest request
    ) {
        return profileImageService.saveMyImage(authentication.getName(), request);
    }

    @PostMapping("/me/upload")
    public List<UserProfileImageResponse> uploadMyImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageOrder") Integer imageOrder,
            @RequestParam("isMain") Boolean isMain
    ) {
        String imageUrl = fileStorageService.storeProfileImage(
                profileImageService.getUserId(authentication.getName()),
                file
        );

        UserProfileImageRequest request = new UserProfileImageRequest();
        request.setImageUrl(imageUrl);
        request.setImageOrder(imageOrder);
        request.setIsMain(isMain);
        return profileImageService.saveMyImage(authentication.getName(), request);
    }

    @GetMapping("/review-status")
    public ReviewStatusResponse getReviewStatus(Authentication authentication) {
        return profileImageService.getReviewStatus(authentication.getName());
    }

    @PostMapping("/review-submit")
    public ReviewStatusResponse submitReview(Authentication authentication) {
        return profileImageService.submitReview(authentication.getName());
    }
}
