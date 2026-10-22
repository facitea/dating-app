/**
 * 남용 방어 판단에 필요한 요청 정보를 담는 값 객체
 */
package com.dating.backend.service;

public record AbuseRequestContext(
        String ipAddress,
        String userAgent
) {
}
