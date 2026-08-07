package com.demo.ticket.Controller;

import com.demo.ticket.Service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
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


}
