// 閱讀導引：保存已完成訂票的訂單編號與回應，供相同請求重試時回傳原結果。
package com.demo.ticket.Dto.Booking;

import lombok.*;

@Setter
@Getter
@Builder
public class BookingCompleteKey {

    /**
     * -- GETTER --
     *  回傳會員電子郵件。
     * -- SETTER --
     *  設定會員電子郵件。

     */
    private String email;
    /**
     * -- GETTER --
     *  回傳訂票重試識別碼。
     * -- SETTER --
     *  設定訂票重試識別碼。

     */
    private String idempotencyKey;
    /**
     * -- GETTER --
     *  回傳已建立的訂單編號。
     * -- SETTER --
     *  設定已建立的訂單編號。

     */
    private String createdOrderNo;
    /**
     * -- GETTER --
     *  回傳已保存的回應內容。
     * -- SETTER --
     *  設定已保存的回應內容。

     */
    private String body;

}
