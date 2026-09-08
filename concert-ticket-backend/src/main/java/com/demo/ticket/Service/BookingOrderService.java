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
    private final BookingMapper mapper;
    private final BookingCoreMapper core;

    public BookingOrderService(BookingMapper mapper, BookingCoreMapper core) {
        this.mapper = mapper;
        this.core = core;
    }

    @Transactional
    public boolean transition(String orderno, String sessionId, String email, OrderStatus target) {
        BookingSaveTicket ticket = core.findOrder(orderno);
        if (ticket == null || (email != null && !email.equals(ticket.getEmail()))
                || (sessionId != null && !sessionId.equals(ticket.getSession_id()))) {
            if (target == OrderStatus.EXPIRED) return false;
            throw new BookingException("ORDER_NOT_FOUND", "找不到訂單", HttpStatus.NOT_FOUND);
        }
        // All writers lock session before order/seat, avoiding payment/reservation deadlocks.
        core.lockSession(ticket.getSession_id());
        ticket = core.findOrder(orderno);
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
            rows = mapper.dopaypriceTicket(pay);
        } else if (target == OrderStatus.CANCELLED) {
            rows = mapper.cancelTicket(ticket);
        } else if (target == OrderStatus.EXPIRED) {
            rows = mapper.updateTicketExpiredAt(ticket);
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
                ? mapper.dopaypriceUpdateSession(session) : mapper.cancelSession(session));
        BookingException.requireOne(core.transitionSeat(ticket.getSession_id(), ticket.getSeat(), orderno,
                target == OrderStatus.PAID ? "SOLD" : "AVAILABLE"));
        return true;
    }

    private BookingException conflict() {
        return new BookingException("INVALID_ORDER_STATE", "訂單狀態已變更或已逾期", HttpStatus.CONFLICT);
    }
}
