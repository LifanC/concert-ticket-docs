package com.demo.ticket.Exception;

import com.demo.ticket.Dto.ApiErrorResponse;
import com.demo.ticket.security.RateLimitException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/** 集中決定各種例外的狀態與訊息，所有回應由 error() 統一組裝。 */
@RestControllerAdvice
@Slf4j
@NullMarked
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitException exception) {
        ApiErrorResponse body = error(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "RATE_LIMIT_EXCEEDED",
                        "請求過於頻繁，請稍後再試"
        );
        return ResponseEntity
                .status(body.status())
                .header("Retry-After", Long.toString(exception.getRetryAfter()))
                .body(body);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataUnavailable() {
        ApiErrorResponse body = error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SERVICE_UNAVAILABLE",
                "服務暫時無法使用"
        );
        return ResponseEntity
                .status(body.status())
                .header("Retry-After", "5")
                .body(body);
    }

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ApiErrorResponse> handleBooking(BookingException exception) {
        return response(
                error(
                        exception.getStatus(),
                        exception.getCode(),
                        Objects.requireNonNullElse(
                                exception.getMessage(),
                                "訂票操作失敗"
                        )
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new TreeMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(),
                        Objects.requireNonNullElse(
                                fieldError.getDefaultMessage(),
                                "欄位格式不正確")
                )
        );
        return validationError(fieldErrors);
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleFieldValidation(FieldValidationException exception) {
        return validationError(
                Map.of(
                        exception.getField(),
                        Objects.requireNonNullElse(
                                exception.getMessage(),
                                "欄位格式不正確")
                )
        );
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateKey() {
        return response(
                error(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_DATA",
                        "資料已存在")
        );
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtException() {
        return response(
                error(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Token 無效或已過期"
                )
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied() {
        return response(
                error(
                        HttpStatus.FORBIDDEN,
                        "FORBIDDEN",
                        "權限不足"
                )
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleInvalidOperation(RuntimeException exception) {
        return response(
                error(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_OPERATION",
                        Objects.requireNonNullElse(
                                exception.getMessage(),
                                "操作失敗")
                )
        );
    }

    private ResponseEntity<ApiErrorResponse> response(ApiErrorResponse body) {
        return ResponseEntity
                .status(body.status())
                .body(body);
    }

    private ResponseEntity<ApiErrorResponse> validationError(Map<String, String> fieldErrors) {
        ApiErrorResponse body = error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "參數驗證失敗"
        ).withFieldErrors(fieldErrors);
        return response(body);
    }

    private ApiErrorResponse error(HttpStatus status, String code, String message) {
        String traceId = UUID.randomUUID().toString();
        // 不記錄原始 JWT、表單拒絕值或資料庫例外內容。
        log.warn("API error traceId={} status={} code={}", traceId, status.value(), code);
        List<Map<String, Object>> data =
                List.of(
                        Map.of("remark", message),
                        Map.of("error", Map.of())
                );
        return new ApiErrorResponse(status.value(), code, message, traceId, Instant.now(), data);
    }
}
