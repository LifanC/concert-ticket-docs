// 閱讀導引：訂票的資料庫介面；方法對應 resources/Mapper/BookingMapper.xml 內同名 SQL，實際查詢與更新在 XML 中定義。
package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Booking.BookingDopaypriceTicket;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.BookingSalesDate;
import com.demo.ticket.Dto.Booking.BookingSession;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Mapper
public interface BookingMapper {

    /**
     * 查詢活動有場次的不同演出日期，回傳 value、label、note，供訂票第一步的日期選單使用。
     */
    List<Map<String, Object>> selectOnlyActivities(String activity_id);

    /**
     * 查詢活動在指定日期的全部場次，回傳時間與 capacity－reserved－sold；SQL 未按狀態或開賣時間篩選。
     */
    List<Map<String, Object>> selectOnlySession(String date, String activity_id);

    /**
     * 依傳入 email 查詢票券並按訂單編號倒序排列；會員歸屬由服務層提供的 email 控制。
     */
    List<Map<String,Object>> selectOnlyTicket(String email);

    /**
     * 依活動編號查詢活動票價。
     */
    @MapKey("id")
    Map<String, Map<String, Object>> selectOnlyActivitiesPrice(String activity_id);

    /**
     * 僅在 capacity－reserved－sold 至少為 1 時增加 reserved；capacity 是總容量，本方法不直接減少 capacity。
     */
    int updateSession(BookingSession bookingSession);

    /**
     * 按場次編號回傳資料；結果中的 capacity 是剩餘可售數，不是資料表的總容量。
     */
    @MapKey("id")
    Map<String, Map<String, Object>> selectOnlySessionId(String session_id);

    /**
     * 讀取活動的預設票價。
     */
    BigDecimal selectActivityPrice(String activity_id);

    /**
     * 寫入訂單並回傳資料庫產生的訂單編號；交易一致性由服務層負責。
     */
    String saveTicket(BookingSaveTicket bookingSaveTicket);

    /**
     * 保留數至少為 1 時將 reserved 減一，使剩餘可售數增加；回傳更新筆數。
     */
    int cancelSession(BookingSession bookingSession);

    /**
     * 僅更新尚未到付款期限的待付款訂單，並核對訂單、場次及會員；成功寫入 CANCELLED 與取消時間。
     */
    int cancelTicket(BookingSaveTicket bookingSaveTicket);

    /**
     * 查詢訂單所屬場次的售票時間資訊。
     */
    @MapKey("id")
    Map<String, Map<String, Object>> sessionSalesDate(BookingSalesDate bookingSalesDate);

    /**
     * 將場次保留數轉入售出數，回傳更新筆數。
     */
    int dopaypriceUpdateSession(BookingSession bookingSession);

    /**
     * 核對訂單、場次及會員，僅讓尚未逾期的待付款訂單改為 PAID，並以 price 記錄 payprice。
     */
    int dopaypriceTicket(BookingDopaypriceTicket bookingDopaypriceTicket);

    /**
     * 讀取指定訂單目前的狀態。
     */
    String selectTicketStatus(BookingSaveTicket bookingSaveTicket);

    /**
     * 僅讓付款期限已到的待付款訂單改為 EXPIRED，並記錄 cancelled_at；未到期或已處理時回傳 0。
     */
    int updateTicketExpiredAt(BookingSaveTicket bookingSaveTicket);

    /**
     * 讀取活動座位配置，供座位圖或場次建立使用。
     */
    @MapKey("activity_id")
    Map<String, Map<String, Object>> selectOnlySeats(String activity_id);

    /**
     * 舊查詢：按日期與時間讀取待付款或已付款座位，未限定活動與場次；目前服務改用 BookingCoreMapper 的場次查詢。
     */
    List<Map<String, Object>> selectOnlyUnavailableSeats(BookingSaveTicket bookingSaveTicket);

    /**
     * 依活動、日期及時間取得對應場次編號。
     */
    @MapKey("activity_id")
    Map<String, Map<String, Object>> getOnlySessionId(String activityId, String selectedDate, String selectedSession);
}
