// 閱讀導引：售票設定的服務契約；定義呼叫端可使用的操作，實際流程由對應實作類別負責。
package com.demo.ticket.Service.Admin;

import com.demo.ticket.Dto.Admin.Quote;
import com.demo.ticket.Dto.Admin.SalesSettingsRequest;

import java.math.BigDecimal;
import java.util.Map;

public interface SalesSettingsService {
    /**
     * 回傳管理頁使用的完整場次售票設定。
     */
    Map<String, Object> readAdmin(String sessionId);

    /**
     * 回傳訂票頁使用的售票設定，包含指定會員的已購數及剩餘額度。
     */
    Map<String, Object> readBooking(String sessionId, String email);

    /**
     * 儲存指定場次的售票設定，回傳更新後設定。
     */
    Map<String, Object> save(String sessionId, SalesSettingsRequest request);

    /**
     * 依座位與票種取得後端票價並檢查限購；訂票呼叫端須先取得場次鎖並維持交易。
     */
    Quote quote(String sessionId, String seat, String ticketTypeId, String email, BigDecimal defaultPrice);

}
