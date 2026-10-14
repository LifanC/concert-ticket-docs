// 閱讀導引：訂單寫入與狀態更新資料；保存下單票價及分區／票種名稱，使訂單資訊不受後續顯示資料影響。
package com.demo.ticket.Dto.Booking;

import java.math.BigDecimal;
import java.util.Date;

public class BookingSaveTicket {

    private String orderno;
    private String session_id;
    private String email;
    private String name;
    private String date;
    private String time;
    private String status;
    private String seat;
    private BigDecimal price;
    private String zoneId;
    private String ticketTypeId;
    private String zoneName;
    private String ticketTypeName;
    private Date expires_at;
    private Date paid_at;
    private Date cancelled_at;

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
     * 回傳名稱。
     */
    public String getName() {
        return name;
    }

    /**
     * 設定名稱。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 回傳日期。
     */
    public String getDate() {
        return date;
    }

    /**
     * 設定日期。
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * 回傳時間。
     */
    public String getTime() {
        return time;
    }

    /**
     * 設定時間。
     */
    public void setTime(String time) {
        this.time = time;
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
     * 回傳訂單原價。
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * 設定訂單原價。
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * 回傳分區編號。
     */
    public String getZoneId() {
        return zoneId;
    }

    /**
     * 設定分區編號。
     */
    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    /**
     * 回傳票種編號。
     */
    public String getTicketTypeId() {
        return ticketTypeId;
    }

    /**
     * 設定票種編號。
     */
    public void setTicketTypeId(String ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    /**
     * 回傳下單時的分區名稱。
     */
    public String getZoneName() {
        return zoneName;
    }

    /**
     * 設定下單時的分區名稱。
     */
    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    /**
     * 回傳下單時的票種名稱。
     */
    public String getTicketTypeName() {
        return ticketTypeName;
    }

    /**
     * 設定下單時的票種名稱。
     */
    public void setTicketTypeName(String ticketTypeName) {
        this.ticketTypeName = ticketTypeName;
    }

    /**
     * 回傳付款期限。
     */
    public Date getExpires_at() {
        return expires_at;
    }

    /**
     * 設定付款期限。
     */
    public void setExpires_at(Date expires_at) {
        this.expires_at = expires_at;
    }

    /**
     * 回傳付款完成時間。
     */
    public Date getPaid_at() {
        return paid_at;
    }

    /**
     * 設定付款完成時間。
     */
    public void setPaid_at(Date paid_at) {
        this.paid_at = paid_at;
    }

    /**
     * 回傳取消時間。
     */
    public Date getCancelled_at() {
        return cancelled_at;
    }

    /**
     * 設定取消時間。
     */
    public void setCancelled_at(Date cancelled_at) {
        this.cancelled_at = cancelled_at;
    }
}
