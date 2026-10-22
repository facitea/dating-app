/**
 * 프론트가 어떤 입력칸에 포커스를 둘지 힌트를 함께 전달하는 예외
 */
package com.dating.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FocusableResponseStatusException extends ResponseStatusException {

    private final String focusField;

    public FocusableResponseStatusException(HttpStatus status, String reason, String focusField) {
        super(status, reason);
        this.focusField = focusField;
    }

    public String getFocusField() {
        return focusField;
    }
}
