// 閱讀導引：活動與場次管理的 HTTP 入口；接收與驗證請求後委派服務層執行，並組裝回應。
package com.demo.ticket.Controller;

import com.demo.ticket.Dto.Admin.*;
import com.demo.ticket.Service.Admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin API", description = "管理員後台功能")
@RestController
@RequestMapping("/v1/admin")
@Validated
public class AdminController {

    private final AdminService adminService;

    /**
     * 注入管理服務，供活動圖片與場次維護端點使用。
     */
    public AdminController(
            AdminService adminService
    ){
        this.adminService = adminService;
    }

    /**
     * 查詢活動清單，回傳活動資訊供頁面呈現。
     */
    @Operation(summary = "1.活動資料全部", description = "活動資料全部")
    @GetMapping("/selectAllActivities")
    public List<Map<String, Object>> selectAllActivities() {
        return adminService.selectAllActivities();
    }

    /**
     * 查詢管理頁使用的場次清單及售票資訊。
     */
    @Operation(summary = "1.場次資料全部", description = "場次資料全部")
    @GetMapping("/selectAllSessions")
    public List<Map<String, Object>> selectAllSessions() {
        return adminService.selectAllSessions();
    }

    /**
     * 查詢管理頁使用的訂單清單。
     */
    @Operation(summary = "1.售票資料全部", description = "售票資料全部")
    @GetMapping("/selectAllticket")
    public List<Map<String, Object>> selectAllticket() {
        return adminService.selectAllticket();
    }

    /**
     * 接收活動資料與可省略的 JPEG 圖片，交由服務在交易內新增或更新；未上傳圖片時保留既有圖片。
     */
    @Operation(summary = "2.增加、修改活動", description = "增加、修改活動")
    @PostMapping("/saveActivity")
    public Map<String, Object> saveActivity(
            @Valid
            @RequestPart("activity")
            AdminSaveActivityRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return adminService.saveActivity(request, image);
    }

    /**
     * 依活動編號委派刪除；不存在回傳 404，已有場次回傳 409，成功時回傳更新後的活動清單。
     */
    @Operation(summary = "3.刪除活動", description = "刪除活動")
    @DeleteMapping("/deleteActivity")
    public ResponseEntity<?> deleteActivity(
            @Valid
            @RequestBody
            AdminDeleteActivityRequest request) {
        return adminService.deleteActivity(request);
    }

    /**
     * 新增或更新場次；空 id 表示新增，指定 id 表示編輯，回應提供完整場次清單。
     */
    @Operation(summary = "4.新增場次", description = "新增場次")
    @PostMapping("/createSession")
    public ResponseEntity<?> createSession(
            @Valid
            @RequestBody
            AdminCreateSessionRequest request) {
        return adminService.createSession(request);
    }
}
