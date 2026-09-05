package com.demo.ticket.Controller;

import com.demo.ticket.Dto.Activity.ActivityFavoriteRequest;
import com.demo.ticket.Service.ActivityService;
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

    public ActivityController(
            ActivityService activityService
    ){
        this.activityService = activityService;
    }

    @Operation(summary = "1.活動資料全部", description = "活動資料全部")
    @GetMapping("/selectAllActivities")
    public List<Map<String, Object>> selectAllActivities() {
        return activityService.selectAllActivities();
    }

    @Operation(summary = "2.收藏活動資料", description = "收藏活動資料")
    @GetMapping("/selectOnlyFavoriteActivities")
    public List<Map<String, Object>> selectOnlyFavoriteActivities(
            @AuthenticationPrincipal LoginUser user) {
        return activityService.selectOnlyFavoriteActivities(user);
    }

    @Operation(summary = "3.新增收藏活動", description = "新增收藏活動")
    @PostMapping("/saveFavoriteActivity")
    public ResponseEntity<?> saveFavoriteActivity(
            @Valid
            @RequestBody
            ActivityFavoriteRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return activityService.saveFavoriteActivity(request, user);
    }

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
