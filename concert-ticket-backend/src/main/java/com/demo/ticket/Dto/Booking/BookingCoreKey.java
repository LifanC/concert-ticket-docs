// 閱讀導引：建立訂票請求的去重紀錄，將會員、請求識別碼與內容雜湊綁定。
package com.demo.ticket.Dto.Booking;

import lombok.*;

@Setter
@Getter
@Builder
public class BookingCoreKey {

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
     *  回傳請求內容雜湊。
     * -- SETTER --
     *  設定請求內容雜湊。

     */
    private String hash;

}
