// 閱讀導引：定義訂單狀態與允許的轉換，付款、取消及逾期流程共用這組規則。
package com.demo.ticket.Dto.Booking;

public enum bookingOrderStatus {
    PENDING_PAYMENT,
    PAID,
    CANCELLED,
    EXPIRED,
    REFUNDED;

    /**
     * 判斷狀態規則是否允許轉換：待付款可轉為付款、取消或逾期，已付款可轉為退款。
     * 這只描述狀態關係；BookingOrderService 目前仍拒絕 REFUNDED，且付款期限另由 SQL 檢查。
     */
    public boolean canTransitionTo(bookingOrderStatus target) {
        return this == PENDING_PAYMENT &&
                (target == PAID || target == CANCELLED || target == EXPIRED) ||
                this == PAID && target == REFUNDED;
    }
}
