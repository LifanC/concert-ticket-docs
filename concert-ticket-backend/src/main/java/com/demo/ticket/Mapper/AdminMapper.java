// 閱讀導引：活動與場次管理的資料庫介面；方法對應 resources/Mapper/AdminMapper.xml 內同名 SQL，實際查詢與更新在 XML 中定義。
package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Admin.Activity;
import com.demo.ticket.Dto.Admin.Session;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface AdminMapper {

    /**
     * 讀取管理頁活動清單，包含活動座位配置及圖片 API 路徑。
     */
    List<Map<String,Object>> selectAllActivities();

    /**
     * 依活動 id 讀取編輯資料，回傳以 id 為鍵的 Map；找不到時沒有對應鍵值。
     */
    @MapKey("id")
    Map<String, Map<String, Object>> selectOnlyActivities(String id);

    /**
     * 查詢管理頁使用的場次清單及售票資訊。
     */
    List<Map<String,Object>> selectAllSessions();

    /**
     * 查詢管理頁使用的訂單清單。
     */
    List<Map<String, Object>> selectAllticket();

    /**
     * id 空白時產生活動編號，否則以指定 id 新增或更新；回傳 activity_id 與 activity_uuid。
     */
    Map<String, Object> create_activity(Activity activity);

    /**
     * 新增活動圖片；已有圖片時更新其內容及尺寸。
     */
    void upsertActivityImage(UUID activity_uuid, String filename, int width, int height, byte[] image_data);

    /**
     * 僅刪除尚未建立場次的活動圖片，回傳刪除筆數；有場次時不刪除。
     */
    int deleteActivityImage(String id);

    /**
     * 僅刪除沒有任何場次的活動，回傳刪除筆數。
     */
    int delete_activity(String id);
    /**
     * 在交易內鎖定活動；找不到時回傳 null。呼叫端藉此協調刪除與其他遵守此鎖的操作。
     */
    String lockActivity(String id);
    /**
     * 判斷活動是否已有場次，供刪除前檢查使用。
     */
    boolean activityHasSessions(String id);

    /**
     * id 空白時新增場次，否則新增或更新指定場次；Java 宣告為 void，不提供 SQL RETURNING 的 id 給呼叫端。
     */
    void create_session(Session session);
    /**
     * 取得場次資料列鎖；須在交易內呼叫，後續再檢查訂單、座位及庫存。
     */
    void lockSession(String id);
    /**
     * 僅查詢已有售票設定的場次，回傳所屬活動與固定容量，供編輯時保留原配置。
     */
    Map<String, Object> configuredSession(String id);

    /**
     * 建立活動使用的座位配置。
     */
    void create_seat(String id, String activity_id, String seat_rows, BigDecimal seats_per_row);

    /**
     * 刪除指定活動的座位配置。
     */
    void delete_seat_by_activity(String activity_id);

    /**
     * 讀取活動座位配置，供座位圖或場次建立使用。
     */
    @MapKey("activity_id")
    Map<String, Map<String, Object>> selectOnlySeats(String activity_id);
}
