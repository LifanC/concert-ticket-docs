// 閱讀導引：傳遞座位狀態轉換條件；場次、座位及訂單編號共同識別要更新的保留。
package com.demo.ticket.Dto.Booking;

import lombok.*;

@Setter
@Getter
@Builder
public class BookingTransitionSeat {

    /**
     * -- GETTER --
     *  回傳場次編號。
     * -- SETTER --
     *  設定場次編號。

     */
    private String sessionId;
    /**
     * -- GETTER --
     *  回傳座位編號。
     * -- SETTER --
     *  設定座位編號。

     */
    private String seat;
    /**
     * -- GETTER --
     *  回傳訂單編號。
     * -- SETTER --
     *  設定訂單編號。

     */
    private String orderno;
    /**
     * -- GETTER --
     *  回傳業務狀態。
     * -- SETTER --
     *  設定業務狀態。

     */
    private String status;

}
