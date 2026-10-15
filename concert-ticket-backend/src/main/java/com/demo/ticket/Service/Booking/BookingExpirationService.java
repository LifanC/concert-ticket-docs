// 閱讀導引：提供獨立的逾期交易入口，讓付款排程透過 Spring 交易代理呼叫共用訂單轉換。
package com.demo.ticket.Service.Booking;

import com.demo.ticket.Dto.Booking.BookingOrder;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.bookingOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingExpirationService {
    private final BookingOrderService bookingOrderService;

    /**
     * 注入共用訂單轉換服務，供排程經由獨立交易入口處理逾期。
     */
    public BookingExpirationService(
            BookingOrderService bookingOrderService
    ) {
        this.bookingOrderService = bookingOrderService;
    }

    // 排程呼叫另一個 Spring 元件，確保 @Transactional 生效；同一物件內部直接呼叫不會經過代理。
    /**
     * 透過共用狀態轉換將訂單標記為逾期；已處理訂單回傳 false。
     */
    @Transactional
    public boolean expire(BookingSaveTicket ticket) {
        BookingOrder bookingOrder = BookingOrder.builder()
                .orderno(ticket.getOrderno())
                .session_id(ticket.getSession_id())
                .email(null)
                .status(bookingOrderStatus.EXPIRED)
                .build();
        return bookingOrderService.transition(bookingOrder);
    }
}
