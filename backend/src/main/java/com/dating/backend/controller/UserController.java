/**
 * UserController API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.UserResponse;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public UserResponse getMyInfo(Authentication authentication) {
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
    }
}
