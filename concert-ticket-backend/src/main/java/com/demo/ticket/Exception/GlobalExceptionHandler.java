package com.demo.ticket.Exception;

import com.demo.ticket.Dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import io.jsonwebtoken.JwtException;

import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<?> handleBooking(BookingException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(
                        Map.of(
                                "code", ex.getCode(),
                                "message", ex.getMessage(),
                                "traceId", UUID.randomUUID().toString(),
                                "timestamp", Instant.now().toString()
                        )
                );
    }

    // DTO 驗證失敗
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(
            MethodArgumentNotValidException ex) {
        logger.warn("參數驗證失敗: {}", ex.getMessage());
        Map<String, String> fieldErrors = new TreeMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> {
                    fieldErrors.put(
                            error.getField(),
                            error.getDefaultMessage()
                    );
                });

        return validationError(fieldErrors);
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<?> handleFieldValidation(FieldValidationException ex) {
        logger.warn("欄位驗證失敗: {}: {}", ex.getField(), ex.getMessage());
        return validationError(Map.of(ex.getField(), ex.getMessage()));
    }

    private ResponseEntity<?> validationError(Map<String, String> fieldErrors) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<Map<String, Object>> data = List.of(
                Map.of("remark", "參數驗證失敗"),
                Map.of("error", fieldErrors)
        );
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateKey(DuplicateKeyException ex) {
        logger.warn("資料已存在", ex);
        return error(HttpStatus.CONFLICT, "資料已存在");
    }

    @ExceptionHandler({JwtException.class})
    public ResponseEntity<?> handleJwtException(JwtException ex) {
        logger.warn("Token 驗證失敗: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Token 無效或已過期");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("權限不足: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "權限不足");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleInvalidOperation(RuntimeException ex) {
        logger.warn("操作失敗: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.api(status, msg(message))
                );
    }

    private List<Map<String, Object>> msg(String ex) {
        return List.of(Map.of("remark", ex));
    }

}
