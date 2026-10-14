// 閱讀導引：售票設定的 HTTP 入口；接收與驗證請求後委派服務層執行，並組裝回應。
package com.demo.ticket.Controller;

import com.demo.ticket.Dto.Admin.SalesSettingsRequest;
import com.demo.ticket.Service.Admin.SalesSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Sales Settings API", description = "場次分區、票種定價及限購設定")
@RestController
@RequestMapping("/v1/admin")
@Validated
public class SalesSettingsController {
    private final SalesSettingsService salesSettingsService;

    /**
     * 注入售票設定服務，供管理員查詢及儲存場次分區與票種。
     */
    public SalesSettingsController(SalesSettingsService salesSettingsService) {
        this.salesSettingsService = salesSettingsService;
    }

    /**
     * 回傳管理頁使用的完整場次售票設定。
     */
    @Operation(summary = "1.查詢場次銷售設定", description = "管理員查詢分區、票種、定價與限購設定")
    @GetMapping("/sessions/{sessionId}/sales-settings")
    public Map<String, Object> readAdmin(
            @PathVariable String sessionId
    ) {
        return salesSettingsService.readAdmin(sessionId);
    }

    /**
     * 接收場次售票設定，驗證請求後委派服務儲存並回傳結果。
     */
    @Operation(summary = "2.儲存場次銷售設定", description = "管理員設定分區、票種、定價與限購")
    @PutMapping("/sessions/{sessionId}/sales-settings")
    public Map<String, Object> save(
            @PathVariable String sessionId,
            @Valid
            @RequestBody
            SalesSettingsRequest request) {
        return salesSettingsService.save(sessionId, request);
    }

}
