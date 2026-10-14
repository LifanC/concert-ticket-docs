// 閱讀導引：活動與收藏的 HTTP 入口；接收與驗證請求後委派服務層執行，並組裝回應。
package com.demo.ticket.Controller;

import com.demo.ticket.Dto.Activity.ActivityFavoriteRequest;
import com.demo.ticket.Service.Activity.ActivityService;
import com.demo.ticket.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Activity API", description = "活動功能")
@RestController
@RequestMapping("/v1/activity")
@Validated
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 注入活動服務，將 HTTP 活動與收藏請求交給服務層處理。
     */
    public ActivityController(
            ActivityService activityService
    ){
        this.activityService = activityService;
    }

    /**
     * 查詢活動清單，回傳活動資訊供頁面呈現。
     */
    @Operation(summary = "1.活動資料全部", description = "活動資料全部")
    @GetMapping("/selectAllActivities")
    public List<Map<String, Object>> selectAllActivities() {
        return activityService.selectAllActivities();
    }

    /**
     * 取得活動圖片及 HTTP 回應；找不到圖片時回傳對應錯誤。
     */
    @GetMapping("/activityImage/{activityId}")
    public ResponseEntity<byte[]> activityImage(
            @PathVariable String activityId) {
        return activityService.activityImage(activityId);
    }

    /**
     * 查詢指定會員收藏的活動。
     */
    @Operation(summary = "2.收藏活動資料", description = "收藏活動資料")
    @GetMapping("/selectOnlyFavoriteActivities")
    public List<Map<String, Object>> selectOnlyFavoriteActivities(
            @AuthenticationPrincipal LoginUser user) {
        return activityService.selectOnlyFavoriteActivities(user);
    }

    /**
     * 新增會員與活動的收藏關聯。
     */
    @Operation(summary = "3.新增收藏活動", description = "新增收藏活動")
    @PostMapping("/saveFavoriteActivity")
    public ResponseEntity<?> saveFavoriteActivity(
            @Valid
            @RequestBody
            ActivityFavoriteRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return activityService.saveFavoriteActivity(request, user);
    }

    /**
     * 移除會員與活動的收藏關聯。
     */
    @Operation(summary = "4.刪除收藏活動", description = "刪除收藏活動")
    @DeleteMapping("/deleteFavoriteActivity")
    public ResponseEntity<?> deleteFavoriteActivity(
            @Valid
            @RequestBody
            ActivityFavoriteRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return activityService.deleteFavoriteActivity(request, user);
    }


}
