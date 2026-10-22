/**
 * GlobalExceptionHandler API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.ApiErrorResponse;
import com.dating.backend.exception.FocusableResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String focusField = exception instanceof FocusableResponseStatusException focusableException
                ? focusableException.getFocusField()
                : inferFocusField(exception.getReason());
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.name(), exception.getReason(), focusField));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        if (fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank()) {
                            return fieldError.getDefaultMessage();
                        }
                        return fieldError.getField() + " 값을 다시 확인해 주세요.";
                    }
                    return error.getDefaultMessage() != null ? error.getDefaultMessage() : "입력값을 다시 확인해 주세요.";
                })
                .orElse("입력값을 다시 확인해 주세요.");

        String focusField = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getField)
                .orElse(inferFocusField(message));

        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_ERROR", message, focusField));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String rawMessage = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : exception.getMessage();

        String message = "중복되었거나 사용할 수 없는 값입니다. 입력값을 다시 확인해 주세요.";
        String focusField = null;
        if (rawMessage != null) {
            if (rawMessage.contains("uk_users_email")) {
                message = "이미 가입된 이메일입니다.";
                focusField = "email";
            } else if (rawMessage.contains("uk_users_nickname")) {
                message = "이미 사용 중인 닉네임입니다.";
                focusField = "nickname";
            } else if (rawMessage.contains("uk_users_phone")) {
                message = "이미 사용 중인 전화번호입니다.";
                focusField = "phone";
            } else if (rawMessage.contains("uk_user_profile_images_main_user")) {
                message = "대표 사진은 1장만 지정할 수 있습니다. 다시 시도해 주세요.";
            } else if (rawMessage.contains("uk_user_profile_images_user_order")) {
                message = "같은 사진 순서에는 1장만 등록할 수 있습니다. 다른 순서를 선택해 주세요.";
            } else if (rawMessage.contains("user_profile_images")) {
                message = "프로필 사진 저장 중 DB 제약 조건이 맞지 않습니다. 사진 순서와 대표 사진 여부를 확인해 주세요.";
            }
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("DUPLICATE_VALUE", message, focusField));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("NOT_FOUND", "요청한 API를 찾을 수 없습니다. 백엔드가 최신 코드로 실행 중인지 확인해 주세요."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiErrorResponse("FILE_TOO_LARGE", "사진 파일은 10MB 이하로 업로드해 주세요."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception) {
        log.error("Unhandled server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_SERVER_ERROR", "잠시 후 다시 시도해 주세요."));
    }

    private String inferFocusField(String message) {
        if (message == null) {
            return null;
        }
        if (message.contains("닉네임")) {
            return "nickname";
        }
        if (message.contains("이메일")) {
            return "email";
        }
        if (message.contains("비밀번호") || message.contains("로그인")) {
            return "password";
        }
        if (message.contains("전화번호") || message.contains("휴대폰")) {
            return "phone";
        }
        if (message.contains("인증")) {
            return "code";
        }
        return null;
    }
}
