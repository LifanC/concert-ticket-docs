package com.demo.ticket.Dto.Booking;

public class BookingOrder {

    private String orderno;
    private String session_id;
    private String email;
    private bookingOrderStatus status;

    public String getOrderno() {
        return orderno;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public bookingOrderStatus getStatus() {
        return status;
    }

    public void setStatus(bookingOrderStatus status) {
        this.status = status;
    }
}
