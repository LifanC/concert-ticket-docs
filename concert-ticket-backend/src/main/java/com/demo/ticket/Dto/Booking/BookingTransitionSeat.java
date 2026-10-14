// 閱讀導引：傳遞座位狀態轉換條件；場次、座位及訂單編號共同識別要更新的保留。
package com.demo.ticket.Dto.Booking;

public class BookingTransitionSeat {

    private String sessionId;
    private String seat;
    private String orderno;
    private String status;

    /**
     * 回傳場次編號。
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 設定場次編號。
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 回傳座位編號。
     */
    public String getSeat() {
        return seat;
    }

    /**
     * 設定座位編號。
     */
    public void setSeat(String seat) {
        this.seat = seat;
    }

    /**
     * 回傳訂單編號。
     */
    public String getOrderno() {
        return orderno;
    }

    /**
     * 設定訂單編號。
     */
    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    /**
     * 回傳業務狀態。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 設定業務狀態。
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
