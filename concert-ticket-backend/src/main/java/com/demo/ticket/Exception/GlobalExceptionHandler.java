package com.demo.ticket.Exception;

import com.demo.ticket.Dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import io.jsonwebtoken.JwtException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // DTO 驗證失敗
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleValidationException(
            MethodArgumentNotValidException ex) {
        logger.error(ex.getMessage(), ex);

        Map<String, String> fieldErrors = new TreeMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> {
                    fieldErrors.put(
                            error.getField(),
                            error.getDefaultMessage()
                    );
                });

        List<Map<String, Object>> data = msg("參數驗證失敗");
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("error", fieldErrors);
        data.add(dataMap);
        HttpStatus status = HttpStatus.BAD_REQUEST;
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
        return ResponseEntity.status(status).body(ApiResponse.api(status, msg(message)));
    }

    private List<Map<String, Object>> msg(String ex) {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("remark", ex);
        data.add(dataMap);
        return data;
    }

}
