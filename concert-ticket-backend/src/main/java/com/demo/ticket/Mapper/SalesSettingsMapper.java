// 閱讀導引：售票設定的資料庫介面；方法對應 resources/Mapper/SalesSettingsMapper.xml 內同名 SQL，實際查詢與更新在 XML 中定義。
package com.demo.ticket.Mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SalesSettingsMapper {
    /**
     * 讀取指定場次及活動的基本資訊。
     */
    Map<String, Object> session(String sessionId);

    /**
     * 讀取已儲存的場次售票設定與版本。
     */
    Map<String, Object> settings(String sessionId);

    /**
     * 依場次讀取座位分區及其售價設定。
     */
    List<Map<String, Object>> zones(String sessionId);

    /**
     * 依場次讀取票種及價格比例設定。
     */
    List<Map<String, Object>> ticketTypes(String sessionId);

    /**
     * 讀取指定場次已建立的座位編號，不會在此產生或新增座位。
     */
    List<String> seats(String sessionId);

    /**
     * 檢查是否存在保留或售出座位，或待付款、已付款訂單；回傳 1 或 0，並非占用座位的總數。
     */
    int occupied(String sessionId);

    /**
     * 計算會員在場次中占用限購額度的票數，包含有效待付款及已付款訂單。
     */
    long memberQuantity(String sessionId, String email);

    /**
     * 新增或更新場次的限購張數、版本與座位配置；完整交易及驗證由售票設定服務控制。
     */
    void saveSettings(Map<String, Object> settings);

    /**
     * 清除場次座位的分區關聯，供交易內重建配置使用。
     */
    void clearSeatZones(String sessionId);

    /**
     * 刪除場次分區設定，供交易內重建配置使用。
     */
    void deleteZones(String sessionId);

    /**
     * 刪除場次票種設定，供交易內重建配置使用。
     */
    void deleteTicketTypes(String sessionId);

    /**
     * 新增單一分區的排數範圍、原價及顯示資訊。
     */
    void insertZone(Map<String, Object> zone);

    /**
     * 新增票種名稱、價格比例、資格說明及排序資訊。
     */
    void insertTicketType(Map<String, Object> type);

    /**
     * 批次新增場次座位；場次與座位編號已存在時略過，保留既有座位狀態。
     */
    void insertSeats(String sessionId, List<String> seats);

    /**
     * 將指定座位清單分配至同一分區。
     */
    void assignZone(String sessionId, String zoneId, List<String> seats);

    /**
     * 核對座位分區與票種都屬於同一場次，回傳分區原價與票種百分比；折扣後金額由服務層計算。
     */
    Map<String, Object> seatPrice(String sessionId, String seat, String ticketTypeId);
}
