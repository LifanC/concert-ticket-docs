// 閱讀導引：供庫存 SQL 定位場次的識別資料，付款、保留及釋放流程共用。
package com.demo.ticket.Dto.Booking;

public class BookingSession {

    private String session_id;

    /**
     * 回傳場次編號。
     */
    public String getSession_id() {
        return session_id;
    }

    /**
     * 設定場次編號。
     */
    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }
}
