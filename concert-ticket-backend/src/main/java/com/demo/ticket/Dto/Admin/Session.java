// 閱讀導引：場次儲存資料；capacity 是總容量，不是當下剩餘可售數。
package com.demo.ticket.Dto.Admin;

import java.math.BigDecimal;

public class Session {

    private String id;
    private String activity_id;
    private String date;
    private String time;
    private String salesdate;
    private String salestime;
    private BigDecimal capacity;
    private String status;

    /**
     * 回傳識別碼。
     */
    public String getId() {
        return id;
    }

    /**
     * 設定識別碼。
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 回傳活動編號。
     */
    public String getActivity_id() {
        return activity_id;
    }

    /**
     * 設定活動編號。
     */
    public void setActivity_id(String activity_id) {
        this.activity_id = activity_id;
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
     * 回傳開賣日期。
     */
    public String getSalesdate() {
        return salesdate;
    }

    /**
     * 設定開賣日期。
     */
    public void setSalesdate(String salesdate) {
        this.salesdate = salesdate;
    }

    /**
     * 回傳開賣時間。
     */
    public String getSalestime() {
        return salestime;
    }

    /**
     * 設定開賣時間。
     */
    public void setSalestime(String salestime) {
        this.salestime = salestime;
    }

    /**
     * 回傳場次容量。
     */
    public BigDecimal getCapacity() {
        return capacity;
    }

    /**
     * 設定場次容量。
     */
    public void setCapacity(BigDecimal capacity) {
        this.capacity = capacity;
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
