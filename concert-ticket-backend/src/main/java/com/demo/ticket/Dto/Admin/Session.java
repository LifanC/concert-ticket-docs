// 閱讀導引：場次儲存資料；capacity 是總容量，不是當下剩餘可售數。
package com.demo.ticket.Dto.Admin;

import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class Session {

    /**
     * -- GETTER --
     *  回傳識別碼。
     * -- SETTER --
     *  設定識別碼。

     */
    private String id;
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
    /**
     * -- GETTER --
     *  回傳開賣日期。
     * -- SETTER --
     *  設定開賣日期。

     */
    private String salesdate;
    /**
     * -- GETTER --
     *  回傳開賣時間。
     * -- SETTER --
     *  設定開賣時間。

     */
    private String salestime;
    /**
     * -- GETTER --
     *  回傳場次容量。
     * -- SETTER --
     *  設定場次容量。

     */
    private BigDecimal capacity;
    /**
     * -- GETTER --
     *  回傳業務狀態。
     * -- SETTER --
     *  設定業務狀態。

     */
    private String status;

}
