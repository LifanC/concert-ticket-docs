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

    public SalesSettingsController(SalesSettingsService salesSettingsService) {
        this.salesSettingsService = salesSettingsService;
    }

    @Operation(summary = "1.查詢場次銷售設定", description = "管理員查詢分區、票種、定價與限購設定")
    @GetMapping("/sessions/{sessionId}/sales-settings")
    public Map<String, Object> readAdmin(
            @PathVariable String sessionId
    ) {
        return salesSettingsService.readAdmin(sessionId);
    }

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
