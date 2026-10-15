// 閱讀導引：供售票時間 SQL 比對場次、活動、演出日期及時間的查詢條件。
package com.demo.ticket.Dto.Booking;

import lombok.*;

@Setter
@Getter
@Builder
public class BookingSalesDate {

    /**
     * -- GETTER --
     *  回傳場次編號。
     * -- SETTER --
     *  設定場次編號。

     */
    private String session_id;
    /**
     * -- GETTER --
     *  回傳活動編號。
     * -- SETTER --
     *  設定活動編號。

     */
    private String activity_id;
    /**
     * -- GETTER --
     *  回傳日期。
     * -- SETTER --
     *  設定日期。

     */
    private String date;
    /**
     * -- GETTER --
     *  回傳時間。
     * -- SETTER --
     *  設定時間。

     */
    private String time;

}
