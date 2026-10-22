/**
 * 비용 없이 테스트할 수 있도록 SMS 인증번호를 서버 로그에 남기는 개발용 발송기
 */
package com.dating.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class DevPhoneSmsSender implements PhoneSmsSender {

    // 실제 문자 발송 대신 bootRun 콘솔 로그에 인증번호를 출력한다.
    @Override
    public void sendVerificationCode(String phone, String code, LocalDateTime expiresAt) {
        log.info("[DEV SMS AUTH] phone={}, code={}, expiresAt={}", phone, code, expiresAt);
    }
}
