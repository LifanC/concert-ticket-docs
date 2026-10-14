// 閱讀導引：供付款 SQL 核對訂單、場次及會員 email；customer 欄位存的是會員 email。
package com.demo.ticket.Dto.Booking;

import java.util.Date;

public class BookingDopaypriceTicket {

    private String orderno;
    private String session_id;
    private String customer;
    private String date;
    private String time;
    private String status;
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
     * 回傳付款 SQL 要核對的會員 email；此欄位不是姓名。
     */
    public String getCustomer() {
        return customer;
    }

    /**
     * 設定付款 SQL 用來比對 ticket.email 的會員 email。
     */
    public void setCustomer(String customer) {
        this.customer = customer;
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
