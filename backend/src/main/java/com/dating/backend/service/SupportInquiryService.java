/**
 * 고객센터 문의 등록과 조회를 처리합니다.
 */
package com.dating.backend.service;

import com.dating.backend.dto.SupportInquiryCreateRequest;
import com.dating.backend.dto.SupportInquiryResponse;
import com.dating.backend.entity.SupportInquiry;
import com.dating.backend.entity.User;
import com.dating.backend.repository.SupportInquiryRepository;
import com.dating.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportInquiryService {

    private final SupportInquiryRepository supportInquiryRepository;
    private final UserRepository userRepository;

    /**
     * 로그인 사용자의 문의를 등록합니다.
     */
    @Transactional
    public SupportInquiryResponse createInquiry(String email, SupportInquiryCreateRequest request) {
        User user = getUserByEmail(email);

        SupportInquiry inquiry = supportInquiryRepository.save(SupportInquiry.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .category(request.getCategory().trim())
                .channel(request.getChannel().trim())
                .status("OPEN")
                .question(request.getQuestion().trim())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        return SupportInquiryResponse.from(inquiry);
    }

    /**
     * 로그인 사용자의 최근 문의 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<SupportInquiryResponse> getMyInquiries(String email) {
        User user = getUserByEmail(email);
        return supportInquiryRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(SupportInquiryResponse::from)
                .toList();
    }

    /**
     * 이메일로 사용자 정보를 찾습니다.
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));
    }
}
