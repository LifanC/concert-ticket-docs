// 閱讀導引：供付款 SQL 核對訂單、場次及會員 email；customer 欄位存的是會員 email。
package com.demo.ticket.Dto.Booking;

import lombok.*;

import java.util.Date;

@Setter
@Getter
@Builder
public class BookingDopaypriceTicket {

    /**
     * -- GETTER --
     *  回傳訂單編號。
     * -- SETTER --
     *  設定訂單編號。

     */
    private String orderno;
    /**
     * -- GETTER --
     *  回傳場次編號。
     * -- SETTER --
     *  設定場次編號。

     */
    private String session_id;
    /**
     * -- GETTER --
     *  回傳付款 SQL 要核對的會員 email；此欄位不是姓名。
     * -- SETTER --
     *  設定付款 SQL 用來比對 ticket.email 的會員 email。

     */
    private String customer;
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
     *  回傳業務狀態。
     * -- SETTER --
     *  設定業務狀態。

     */
    private String status;
    /**
     * -- GETTER --
     *  回傳付款期限。
     * -- SETTER --
     *  設定付款期限。

     */
    private Date expires_at;
    /**
     * -- GETTER --
     *  回傳付款完成時間。
     * -- SETTER --
     *  設定付款完成時間。

     */
    private Date paid_at;
    /**
     * -- GETTER --
     *  回傳取消時間。
     * -- SETTER --
     *  設定取消時間。

     */
    private Date cancelled_at;

}
