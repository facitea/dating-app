/**
 * ScamMonitorSummaryResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScamMonitorSummaryResponse {

// ?袁⑤굡 揶?
    private long highRiskUserCount;
// ?袁⑤굡 揶?
    private long watchUserCount;
// ?袁⑤굡 揶?
    private long openReportCount;
// ?袁⑤굡 揶?
    private long resolvedReportCount;
}