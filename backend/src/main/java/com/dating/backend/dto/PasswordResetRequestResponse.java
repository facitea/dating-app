/**
 * PasswordResetRequestResponse 요청/응답 DTO
 */
package com.dating.backend.dto;

import lombok.Getter;

@Getter
public class PasswordResetRequestResponse extends MessageResponse {

    private final String resetToken;

    public PasswordResetRequestResponse(String message, String resetToken) {
        super(message);
        this.resetToken = resetToken;
    }
}