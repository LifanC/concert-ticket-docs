package com.demo.ticket.Dto.Booking;

public enum bookingOrderStatus {
    PENDING_PAYMENT,
    PAID,
    CANCELLED,
    EXPIRED,
    REFUNDED
    ;

    public boolean canTransitionTo(bookingOrderStatus target) {
        return this == PENDING_PAYMENT &&
                (target == PAID || target == CANCELLED || target == EXPIRED) ||
                this == PAID && target == REFUNDED;
    }
}
