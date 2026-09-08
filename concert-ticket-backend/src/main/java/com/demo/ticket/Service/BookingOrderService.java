package com.demo.ticket.Service;

import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Mapper.BookingCoreMapper;
import com.demo.ticket.Mapper.BookingMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingOrderService {
    private final BookingMapper bookingMapper;
    private final BookingCoreMapper bookingCoreMapper;

    public BookingOrderService(
            BookingMapper bookingMapper,
            BookingCoreMapper bookingCoreMapper
    ) {
        this.bookingMapper = bookingMapper;
        this.bookingCoreMapper = bookingCoreMapper;
    }

    @Transactional
    public boolean transition(String orderno, String sessionId, String email, OrderStatus target) {
        BookingSaveTicket ticket = bookingCoreMapper.findOrder(orderno);
        if (ticket == null || (email != null && !email.equals(ticket.getEmail()))
                || (sessionId != null && !sessionId.equals(ticket.getSession_id()))) {
            if (target == OrderStatus.EXPIRED) return false;
            throw new BookingException("ORDER_NOT_FOUND", "找不到訂單", HttpStatus.NOT_FOUND);
        }
        // All writers lock session before order/seat, avoiding payment/reservation deadlocks.
        bookingCoreMapper.lockSession(ticket.getSession_id());
        ticket = bookingCoreMapper.findOrder(orderno);
        OrderStatus current = OrderStatus.valueOf(ticket.getStatus());
        if (!current.canTransitionTo(target)) {
            if (target == OrderStatus.EXPIRED) return false;
            throw conflict();
        }
        int rows;
        if (target == OrderStatus.PAID) {
            BookingDopaypriceTicket pay = new BookingDopaypriceTicket();
            pay.setOrderno(orderno);
            pay.setSession_id(ticket.getSession_id());
            pay.setCustomer(ticket.getEmail());
            rows = bookingMapper.dopaypriceTicket(pay);
        } else if (target == OrderStatus.CANCELLED) {
            rows = bookingMapper.cancelTicket(ticket);
        } else if (target == OrderStatus.EXPIRED) {
            rows = bookingMapper.updateTicketExpiredAt(ticket);
        } else {
            // Refund accounting is not implemented yet; never expose a partial refund.
            throw conflict();
        }
        if (rows == 0) {
            if (target == OrderStatus.EXPIRED) return false;
            throw conflict();
        }
        BookingException.requireOne(rows);
        BookingSession session = new BookingSession();
        session.setSession_id(ticket.getSession_id());
        BookingException.requireOne(target == OrderStatus.PAID
                ? bookingMapper.dopaypriceUpdateSession(session) : bookingMapper.cancelSession(session));
        BookingException.requireOne(bookingCoreMapper.transitionSeat(ticket.getSession_id(), ticket.getSeat(), orderno,
                target == OrderStatus.PAID ? "SOLD" : "AVAILABLE"));
        return true;
    }

    private BookingException conflict() {
        return new BookingException("INVALID_ORDER_STATE", "訂單狀態已變更或已逾期", HttpStatus.CONFLICT);
    }
}
