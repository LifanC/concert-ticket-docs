// 閱讀導引：訂單寫入與狀態更新資料；保存下單票價及分區／票種名稱，使訂單資訊不受後續顯示資料影響。
package com.demo.ticket.Dto.Booking;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Setter
@Getter
@Builder
public class BookingSaveTicket {

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
     *  回傳會員電子郵件。
     * -- SETTER --
     *  設定會員電子郵件。

     */
    private String email;
    /**
     * -- GETTER --
     *  回傳名稱。
     * -- SETTER --
     *  設定名稱。

     */
    private String name;
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
     *  回傳座位編號。
     * -- SETTER --
     *  設定座位編號。

     */
    private String seat;
    /**
     * -- GETTER --
     *  回傳訂單原價。
     * -- SETTER --
     *  設定訂單原價。

     */
    private BigDecimal price;
    /**
     * -- GETTER --
     *  回傳分區編號。
     * -- SETTER --
     *  設定分區編號。

     */
    private String zoneId;
    /**
     * -- GETTER --
     *  回傳票種編號。
     * -- SETTER --
     *  設定票種編號。

     */
    private String ticketTypeId;
    /**
     * -- GETTER --
     *  回傳下單時的分區名稱。
     * -- SETTER --
     *  設定下單時的分區名稱。

     */
    private String zoneName;
    /**
     * -- GETTER --
     *  回傳下單時的票種名稱。
     * -- SETTER --
     *  設定下單時的票種名稱。

     */
    private String ticketTypeName;
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
