package com.demo.ticket.Dto.Booking;

import java.math.BigDecimal;

public class BookingSession {

    private String session_id;
    private BigDecimal quantity;

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
