// 閱讀導引：集中處理控制器例外；一般錯誤使用 ApiResponse，訂票例外另回傳 code、message、traceId 及 timestamp。
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

    /**
     * 將訂票例外的錯誤代碼、訊息與 HTTP 狀態轉為 API 回應。
     */
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
    /**
     * 收集輸入驗證錯誤，整理成表單欄位可對應的 HTTP 400 回應。
     */
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

    /**
     * 將指定欄位的驗證例外轉成表單錯誤回應。
     */
    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<?> handleFieldValidation(FieldValidationException ex) {
        logger.warn("欄位驗證失敗: {}: {}", ex.getField(), ex.getMessage());
        return validationError(Map.of(ex.getField(), ex.getMessage()));
    }

    /**
     * 回傳 HTTP 400；data[0].remark 說明驗證失敗，data[1].error 以欄位名稱對應錯誤文字，供表單定位。
     */
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

    /**
     * 將資料庫唯一鍵衝突轉為使用者可理解的錯誤回應。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateKey(DuplicateKeyException ex) {
        logger.warn("資料已存在", ex);
        return error(HttpStatus.CONFLICT, "資料已存在");
    }

    /**
     * 將 JWT 驗證例外轉成未授權回應，讓前端處理登入失效。
     */
    @ExceptionHandler({JwtException.class})
    public ResponseEntity<?> handleJwtException(JwtException ex) {
        logger.warn("Token 驗證失敗: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Token 無效或已過期");
    }

    /**
     * 將權限不足例外轉為 HTTP 403 回應。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("權限不足: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "權限不足");
    }

    /**
     * 將 IllegalArgumentException 與 IllegalStateException 轉為 HTTP 400，使用例外訊息作為回應 remark。
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleInvalidOperation(RuntimeException ex) {
        logger.warn("操作失敗: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 依 HTTP 狀態及訊息組裝共用錯誤回應。
     */
    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.api(status, msg(message))
                );
    }

    /**
     * 將錯誤訊息整理成 API 使用的資料清單。
     */
    private List<Map<String, Object>> msg(String ex) {
        return List.of(Map.of("remark", ex));
    }

}
