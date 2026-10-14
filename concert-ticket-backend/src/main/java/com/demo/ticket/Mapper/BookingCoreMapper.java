// 閱讀導引：座位保留及請求去重的資料庫介面；方法對應 resources/Mapper/BookingCoreMapper.xml 內同名 SQL，實際查詢與更新在 XML 中定義。
package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Booking.BookingCompleteKey;
import com.demo.ticket.Dto.Booking.BookingCoreKey;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.BookingTransitionSeat;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookingCoreMapper {
    /**
     * 依編號讀取訂單，flushCache 讓取得場次鎖後的第二次查詢不沿用 MyBatis 本地快取；不存在回傳 null。
     */
    BookingSaveTicket findOrder(String orderno);

    /**
     * 取得場次資料列鎖；須在交易內呼叫，後續再檢查訂單、座位及庫存。
     */
    void lockSession(String sessionId);

    /**
     * 讀取場次與活動的目前資料，供訂票時確認場次及預設票價。
     */
    Map<String, Object> sessionSnapshot(String sessionId);

    /**
     * 僅在座位符合活動排號與每排座位數時補建場次座位；重複資料略過，不合法座位不會被建立。
     */
    void ensureSeat(String sessionId, String seat);

    /**
     * 僅將 AVAILABLE 座位改為 RESERVED，綁定訂單及付款期限；更新 1 筆為成功，0 筆為不存在或已占用。
     */
    int reserveSeat(BookingSaveTicket ticket);

    /**
     * 僅更新仍由指定訂單保留的 RESERVED 座位；售出保留訂單關聯，釋放則清空關聯，兩者皆清除保留期限。
     */
    int transitionSeat(BookingTransitionSeat bookingTransitionSeat);

    /**
     * 取得最早到期的 100 筆待付款訂單編號，供每輪補償掃描逐筆處理。
     */
    List<String> expiredOrders();

    /**
     * 查詢場次目前已保留或售出的座位編號。
     */
    List<String> unavailableSeats(String sessionId);

    /**
     * 以會員 email 與識別碼嘗試新增請求紀錄；唯一鍵衝突時不新增，回傳 0 讓服務讀取先前結果。
     */
    int claimKey(BookingCoreKey bookingCoreKey);

    /**
     * 讀取同一會員與識別碼的請求紀錄並取得資料列鎖，供核對請求雜湊及重用成功回應。
     */
    Map<String, Object> findKey(BookingCoreKey bookingCoreKey);

    /**
     * 僅更新尚未綁定訂單的去重紀錄，保存訂單編號與完整 JSON 回應；回傳更新筆數。
     */
    int completeKey(BookingCompleteKey bookingCompleteKey);
}
