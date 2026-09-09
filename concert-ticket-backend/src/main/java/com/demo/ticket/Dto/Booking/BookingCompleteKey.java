package com.demo.ticket.Dto.Booking;

public class BookingCompleteKey {

    private String email;
    private String idempotencyKey;
    private String createdOrderNo;
    private String body;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCreatedOrderNo() {
        return createdOrderNo;
    }

    public void setCreatedOrderNo(String createdOrderNo) {
        this.createdOrderNo = createdOrderNo;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
