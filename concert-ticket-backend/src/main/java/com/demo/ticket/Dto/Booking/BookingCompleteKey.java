// 閱讀導引：保存已完成訂票的訂單編號與回應，供相同請求重試時回傳原結果。
package com.demo.ticket.Dto.Booking;

public class BookingCompleteKey {

    private String email;
    private String idempotencyKey;
    private String createdOrderNo;
    private String body;

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
     * 回傳已建立的訂單編號。
     */
    public String getCreatedOrderNo() {
        return createdOrderNo;
    }

    /**
     * 設定已建立的訂單編號。
     */
    public void setCreatedOrderNo(String createdOrderNo) {
        this.createdOrderNo = createdOrderNo;
    }

    /**
     * 回傳已保存的回應內容。
     */
    public String getBody() {
        return body;
    }

    /**
     * 設定已保存的回應內容。
     */
    public void setBody(String body) {
        this.body = body;
    }
}
