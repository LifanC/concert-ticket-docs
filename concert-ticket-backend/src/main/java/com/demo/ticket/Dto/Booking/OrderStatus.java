package com.demo.ticket.Dto.Booking;

public enum OrderStatus {
    PENDING_PAYMENT, PAID, CANCELLED, EXPIRED, REFUNDED;

    public boolean canTransitionTo(OrderStatus target) {
        return this == PENDING_PAYMENT && (target == PAID || target == CANCELLED || target == EXPIRED)
                || this == PAID && target == REFUNDED;
    }
}
