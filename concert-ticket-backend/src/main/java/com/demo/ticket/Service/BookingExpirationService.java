package com.demo.ticket.Service;

import com.demo.ticket.Dto.Booking.BookingOrder;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.bookingOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingExpirationService {
    private final BookingOrderService bookingOrderService;

    public BookingExpirationService(
            BookingOrderService bookingOrderService
    ) {
        this.bookingOrderService = bookingOrderService;
    }

    // Separate bean so scheduler calls pass through Spring's transaction proxy.
    // 將 bean 分開，以便調度器呼叫通過 Spring 的事務代理。
    @Transactional
    public boolean expire(BookingSaveTicket ticket) {
        BookingOrder bookingOrder = new BookingOrder();
        bookingOrder.setOrderno(ticket.getOrderno());
        bookingOrder.setSession_id(ticket.getSession_id());
        bookingOrder.setEmail(null);
        bookingOrder.setStatus(bookingOrderStatus.EXPIRED);
        return bookingOrderService.transition(bookingOrder);
    }
}
