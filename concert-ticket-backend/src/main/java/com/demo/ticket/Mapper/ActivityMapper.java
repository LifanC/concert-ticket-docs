// 閱讀導引：活動與收藏的資料庫介面；方法對應 resources/Mapper/ActivityMapper.xml 內同名 SQL，實際查詢與更新在 XML 中定義。
package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Activity.ActivityFavorite;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {

    /**
     * 查詢已有場次的活動及場次狀態；DISTINCT 去除相同資料，同一活動不同狀態仍可保留多筆。
     */
    List<Map<String, Object>> selectAllActivities();

    /**
     * 依活動編號讀取 image_data；圖片 HTTP 類型由活動服務設定為 JPEG。
     */
    Map<String, Object> selectActivityImage(String activityId);

    /**
     * 查詢指定會員收藏的活動。
     */
    List<Map<String, Object>> selectOnlyFavoriteActivities(ActivityFavorite activityFavorite);

    /**
     * 新增會員與活動的收藏；重複收藏以 ON CONFLICT DO NOTHING 略過，回傳 0。
     */
    int saveFavoriteActivity(ActivityFavorite activityFavorite);

    /**
     * 移除會員與活動的收藏關聯。
     */
    int deleteFavoriteActivity(ActivityFavorite activityFavorite);

}
