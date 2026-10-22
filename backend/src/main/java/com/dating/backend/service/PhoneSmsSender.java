/**
 * 휴대폰 SMS 인증번호 발송 방식을 추상화하는 인터페이스
 */
package com.dating.backend.service;

import java.time.LocalDateTime;

public interface PhoneSmsSender {

    // 인증번호를 사용자 휴대폰으로 발송하거나 개발 로그에 남긴다.
    void sendVerificationCode(String phone, String code, LocalDateTime expiresAt);
}
