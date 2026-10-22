/**
 * 관리자 고객 문의 요약 응답 DTO입니다.
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminSupportInquirySummaryResponse {

    private long openCount;
    private long inProgressCount;
    private long resolvedCount;
    private long closedCount;
}
