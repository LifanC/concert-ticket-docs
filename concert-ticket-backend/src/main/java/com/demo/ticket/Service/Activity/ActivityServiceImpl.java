// 閱讀導引：查詢活動與圖片，並處理會員收藏活動的讀取與儲存。
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

    /**
     * 注入活動與收藏資料的查詢介面，讓公開活動和會員收藏共用資料存取元件。
     */
    public ActivityServiceImpl(
            ActivityMapper activityMapper
    ) {
        this.activityMapper = activityMapper;
    }

    /**
     * 讀取已有場次的公開活動；同一活動若各場次狀態不同，SQL 可能回傳不同狀態的多筆活動資料。
     */
    @Override
    public List<Map<String, Object>> selectAllActivities() {
        return activityMapper.selectAllActivities();
    }

    /**
     * 依活動編號讀取 JPEG 位元組，回傳 image/jpeg；查無資料或內容不是 byte[] 時回傳 404。
     */
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

    /**
     * 查詢指定會員收藏的活動。
     */
    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyFavoriteActivities(LoginUser user) {
        List<Map<String, Object>> data = new ArrayList<>();
        if (user != null && Boolean.TRUE.equals(user.accessExists())) {
            ActivityFavorite activityFavorite = ActivityFavorite.builder()
                    .email(user.email())
                    .build();
            data = activityMapper.selectOnlyFavoriteActivities(activityFavorite);
        }
        return data;
    }

    /**
     * 新增會員與活動的收藏關聯。
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveFavoriteActivity(ActivityFavoriteRequest request, LoginUser user) {
        return changeFavoriteActivity(request, user, true);
    }

    /**
     * 移除會員與活動的收藏關聯。
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> deleteFavoriteActivity(ActivityFavoriteRequest request, LoginUser user) {
        return changeFavoriteActivity(request, user, false);
    }

    /**
     * 以登入 email 與活動編號新增或刪除收藏，依更新筆數設定 judge；新增回傳 201，刪除回傳 200。
     */
    private ResponseEntity<?> changeFavoriteActivity(ActivityFavoriteRequest request, LoginUser user, boolean save) {
        final String activity_id = request.activity_id().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap = new HashMap<>();
        ActivityFavorite activityFavorite = ActivityFavorite.builder()
                .email(user.email())
                .activity_id(activity_id)
                .build();
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
