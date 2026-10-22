/**
 * ApiErrorResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.Getter;

@Getter
public class ApiErrorResponse {

    // 에러 코드
    private String code;
    // 에러 메시지
    private String message;
    // 프론트에서 포커스를 이동할 입력 필드
    private String focusField;

    public ApiErrorResponse(String code, String message) {
        this(code, message, null);
    }

    public ApiErrorResponse(String code, String message, String focusField) {
        this.code = code;
        this.message = message;
        this.focusField = focusField;
    }
}
