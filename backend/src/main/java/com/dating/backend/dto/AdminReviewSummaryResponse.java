/**
 * AdminReviewSummaryResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminReviewSummaryResponse {

// ?袁⑤굡 揶?
    private long pendingReviewCount;
// ?袁⑤굡 揶?
    private long rejectedCount;
// ?袁⑤굡 揶?
    private long activeCount;
// ?袁⑤굡 揶?
    private long dueSoonCount;
}