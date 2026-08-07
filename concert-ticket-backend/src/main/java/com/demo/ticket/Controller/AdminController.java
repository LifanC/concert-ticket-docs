package com.demo.ticket.Controller;

import com.demo.ticket.Dto.Admin.*;
import com.demo.ticket.Service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin API", description = "管理員後台功能")
@RestController
@RequestMapping("/v1/admin")
@Validated
public class AdminController {

    private final AdminService adminService;

    public AdminController(
            AdminService adminService){
        this.adminService = adminService;
    }

    @Operation(summary = "1.活動資料全部", description = "活動資料全部")
    @GetMapping("/selectAllActivities")
    public List<Map<String, Object>> selectAllActivities() {
        return adminService.selectAllActivities();
    }

    @Operation(summary = "1.場次資料全部", description = "場次資料全部")
    @GetMapping("/selectAllSessions")
    public List<Map<String, Object>> selectAllSessions() {
        return adminService.selectAllSessions();
    }

    @Operation(summary = "1.售票資料全部", description = "售票資料全部")
    @GetMapping("/selectAllticket")
    public List<Map<String, Object>> selectAllticket() {
        return adminService.selectAllticket();
    }

    @Operation(summary = "2.增加、修改活動", description = "增加、修改活動")
    @PostMapping("/saveActivity")
    public ResponseEntity<?> saveActivity(
            @Valid
            @RequestBody
            AdminSaveActivityRequest request,
            @Schema(description = "token", example = "token_abc123", requiredMode = Schema.RequiredMode.REQUIRED)
            @RequestHeader("Authorization") String authHeader) {
        request.setAuthHeader(authHeader);
        return adminService.saveActivity(request);
    }

    @Operation(summary = "3.刪除活動", description = "刪除活動")
    @DeleteMapping("/deleteActivity")
    public ResponseEntity<?> deleteActivity(
            @Valid
            @RequestBody
            AdminDeleteActivityRequest request,
            @Schema(description = "token", example = "token_abc123", requiredMode = Schema.RequiredMode.REQUIRED)
            @RequestHeader("Authorization") String authHeader) {
        request.setAuthHeader(authHeader);
        return adminService.deleteActivity(request);
    }

    @Operation(summary = "4.新增場次", description = "新增場次")
    @PostMapping("/createSession")
    public ResponseEntity<?> createSession(
            @Valid
            @RequestBody
            AdminCreateSessionRequest request,
            @Schema(description = "token", example = "token_abc123", requiredMode = Schema.RequiredMode.REQUIRED)
            @RequestHeader("Authorization") String authHeader) {
        request.setAuthHeader(authHeader);
        return adminService.createSession(request);
    }
}
