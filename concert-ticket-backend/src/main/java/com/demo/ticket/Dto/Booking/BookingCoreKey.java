// 閱讀導引：建立訂票請求的去重紀錄，將會員、請求識別碼與內容雜湊綁定。
package com.demo.ticket.Dto.Booking;

public class BookingCoreKey {

    private String email;
    private String idempotencyKey;
    private String hash;

    /**
     * 回傳會員電子郵件。
     */
    public String getEmail() {
        return email;
    }

    /**
     * 設定會員電子郵件。
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 回傳訂票重試識別碼。
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * 設定訂票重試識別碼。
     */
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * 回傳請求內容雜湊。
     */
    public String getHash() {
        return hash;
    }

    /**
     * 設定請求內容雜湊。
     */
    public void setHash(String hash) {
        this.hash = hash;
    }
}
