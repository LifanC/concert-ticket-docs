// 閱讀導引：統一 API 回應的狀態與資料格式，讓前端以一致方式解析結果。
package com.demo.ticket.Dto;

import com.demo.ticket.Common.ConvertFormat;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class ApiResponse {

    /**
     * 將 HttpStatus、數值 status、data 清單與台北時間 timestamp 組成回應 Map；HTTP 狀態仍由呼叫端 ResponseEntity 設定。
     */
    public static Map<String, Object> api(HttpStatus code, List<Map<String, Object>> data) {
        return Map.of(
                "code", code,
                "status", code.value(),
                "data", data,
                "timestamp", ConvertFormat.now()
        );
    }

}
