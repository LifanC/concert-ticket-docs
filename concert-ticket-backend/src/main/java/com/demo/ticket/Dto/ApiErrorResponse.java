package com.demo.ticket.Dto;

import org.jspecify.annotations.NullMarked;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 所有控制器例外共用的格式；data 保留既有前端表單的欄位錯誤位置。 */
@NullMarked
public record ApiErrorResponse(
        int status,
        String code,
        String message,
        String traceId,
        Instant timestamp,
        List<Map<String, Object>> data
) {
    /** 加入表單欄位錯誤，同時保留回應的狀態、識別碼與時間。 */
    public ApiErrorResponse withFieldErrors(Map<String, String> fieldErrors) {
        List<Map<String, Object>> validationData =
                List.of(
                        Map.of(
                                "remark",
                                message
                        ),
                        Map.of(
                                "error",
                                fieldErrors
                        )
                );
        return new ApiErrorResponse(status, code, message, traceId, timestamp, validationData);
    }
}
