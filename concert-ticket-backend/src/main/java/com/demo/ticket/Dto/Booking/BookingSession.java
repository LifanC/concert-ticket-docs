// 閱讀導引：供庫存 SQL 定位場次的識別資料，付款、保留及釋放流程共用。
package com.demo.ticket.Dto.Booking;

import lombok.*;

@Setter
@Getter
@Builder
public class BookingSession {

    /**
     * -- GETTER --
     *  回傳場次編號。
     * -- SETTER --
     *  設定場次編號。

     */
    private String session_id;

}
