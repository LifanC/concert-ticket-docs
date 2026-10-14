// 閱讀導引：活動與收藏的服務契約；定義呼叫端可使用的操作，實際流程由對應實作類別負責。
package com.demo.ticket.Service.Activity;

import com.demo.ticket.Dto.Activity.ActivityFavoriteRequest;
import com.demo.ticket.security.LoginUser;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

public interface ActivityService {

    /**
     * 查詢活動清單，回傳活動資訊供頁面呈現。
     */
    List<Map<String, Object>> selectAllActivities();

    /**
     * 取得活動圖片及 HTTP 回應；找不到圖片時回傳對應錯誤。
     */
    ResponseEntity<byte[]> activityImage(String activityId);

    /**
     * 查詢指定會員收藏的活動。
     */
    List<Map<String, Object>> selectOnlyFavoriteActivities(LoginUser user);

    /**
     * 新增會員與活動的收藏關聯。
     */
    ResponseEntity<?> saveFavoriteActivity(ActivityFavoriteRequest request, LoginUser user);

    /**
     * 移除會員與活動的收藏關聯。
     */
    ResponseEntity<?> deleteFavoriteActivity(ActivityFavoriteRequest request, LoginUser user);

}
