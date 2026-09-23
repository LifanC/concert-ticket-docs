package com.demo.ticket.Service.Activity;

import com.demo.ticket.Dto.Activity.ActivityFavorite;
import com.demo.ticket.Dto.Activity.ActivityFavoriteRequest;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Mapper.ActivityMapper;
import com.demo.ticket.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;

    public ActivityServiceImpl(
            ActivityMapper activityMapper
    ) {
        this.activityMapper = activityMapper;
    }

    @Override
    public List<Map<String, Object>> selectAllActivities() {
        return activityMapper.selectAllActivities();
    }

    @Override
    public ResponseEntity<byte[]> activityImage(String activityId) {
        Map<String, Object> image = activityMapper.selectActivityImage(activityId);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        Object imageData = image.get("image_data");
        if (!(imageData instanceof byte[] bytes)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT') or hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyFavoriteActivities(LoginUser user) {
        List<Map<String, Object>> data = new ArrayList<>();
        if (user != null && Boolean.TRUE.equals(user.accessExists())) {
            ActivityFavorite activityFavorite =  new ActivityFavorite();
            activityFavorite.setEmail(user.email());
            data = activityMapper.selectOnlyFavoriteActivities(activityFavorite);
        }
        return data;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveFavoriteActivity(ActivityFavoriteRequest request, LoginUser user) {
        return changeFavoriteActivity(request, user, true);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> deleteFavoriteActivity(ActivityFavoriteRequest request, LoginUser user) {
        return changeFavoriteActivity(request, user, false);
    }

    private ResponseEntity<?> changeFavoriteActivity(ActivityFavoriteRequest request, LoginUser user, boolean save) {
        final String activity_id = request.activity_id().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap = new HashMap<>();
        ActivityFavorite activityFavorite =  new ActivityFavorite();
        activityFavorite.setEmail(user.email());
        activityFavorite.setActivity_id(activity_id);
        int cnt = save
                ? activityMapper.saveFavoriteActivity(activityFavorite)
                : activityMapper.deleteFavoriteActivity(activityFavorite);
        dataMap.put("judge", cnt > 0);
        data.add(dataMap);
        HttpStatus status = save ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

}
