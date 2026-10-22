/**
 * AdminReviewSearchResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminReviewSearchResponse {

// ?怨밴묶
    private String status;
// ?袁⑤굡 揶?
    private boolean dueSoonOnly;
// ?袁⑤굡 揶?
    private String query;
}