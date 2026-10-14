// 閱讀導引：供售票時間 SQL 比對場次、活動、演出日期及時間的查詢條件。
package com.demo.ticket.Dto.Booking;

public class BookingSalesDate {

    private String session_id;
    private String activity_id;
    private String date;
    private String time;

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
}
