// 閱讀導引：訂單狀態轉換指令；orderno 指定訂單，status 指定目標狀態，email 與 session_id 可作歸屬核對條件。
package com.demo.ticket.Dto.Booking;

import lombok.*;

@Setter
@Getter
@Builder
public class BookingOrder {

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
     *  回傳業務狀態。
     * -- SETTER --
     *  設定業務狀態。

     */
    private bookingOrderStatus status;

}
