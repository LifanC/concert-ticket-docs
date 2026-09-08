package com.demo.ticket.Service;

import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.BookingSession;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Mapper.BookingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingExpirationService {
    private final BookingOrderService orders;

    public BookingExpirationService(BookingOrderService orders) {
        this.orders = orders;
    }

    // Separate bean so scheduler calls pass through Spring's transaction proxy.
    @Transactional
    public boolean expire(BookingSaveTicket ticket) {
        return orders.transition(ticket.getOrderno(), ticket.getSession_id(), null,
                com.demo.ticket.Dto.Booking.OrderStatus.EXPIRED);
    }
}
