// 閱讀導引：訂單狀態轉換指令；orderno 指定訂單，status 指定目標狀態，email 與 session_id 可作歸屬核對條件。
package com.demo.ticket.Dto.Booking;

public class BookingOrder {

    private String orderno;
    private String session_id;
    private String email;
    private bookingOrderStatus status;

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

    /**
     * 回傳會員電子郵件。
     */
    public String getEmail() {
        return email;
    }

    /**
     * 設定會員電子郵件。
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 回傳業務狀態。
     */
    public bookingOrderStatus getStatus() {
        return status;
    }

    /**
     * 設定業務狀態。
     */
    public void setStatus(bookingOrderStatus status) {
        this.status = status;
    }
}
