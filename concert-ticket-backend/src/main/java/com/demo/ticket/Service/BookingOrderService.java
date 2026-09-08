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
        boolean notFound = ticket == null
                || (email != null && !email.equals(ticket.getEmail()))
                || (sessionId != null && !sessionId.equals(ticket.getSession_id()));
        if (notFound) {
            if (target == OrderStatus.EXPIRED) {
                return false;
            }
            throw new BookingException("ORDER_NOT_FOUND", "找不到訂單", HttpStatus.NOT_FOUND);
        }
        // All writers lock session before order/seat, avoiding payment/reservation deadlocks.
        // 所有作者在下單/預訂座位前都會鎖定會話，避免付款/預訂僵局。
        bookingCoreMapper.lockSession(ticket.getSession_id());
        ticket = bookingCoreMapper.findOrder(orderno);
        OrderStatus current = OrderStatus.valueOf(ticket.getStatus());
        if (!current.canTransitionTo(target)) {
            if (target == OrderStatus.EXPIRED) {
                return false;
            }
            throw conflict();
        }
        int rows;
        switch (target) {
            case PAID -> {
                BookingDopaypriceTicket pay = new BookingDopaypriceTicket();
                pay.setOrderno(orderno);
                pay.setSession_id(ticket.getSession_id());
                pay.setCustomer(ticket.getEmail());
                rows = bookingMapper.dopaypriceTicket(pay);
            }
            case CANCELLED -> rows = bookingMapper.cancelTicket(ticket);
            case EXPIRED -> rows = bookingMapper.updateTicketExpiredAt(ticket);
            // Refund accounting is not implemented yet; never expose a partial refund.
            // 退款會計系統尚未啟用；切勿公開部分退款資訊。
            default -> throw conflict();
        }
        if (rows == 0) {
            if (target == OrderStatus.EXPIRED) {
                return false;
            }
            throw conflict();
        }
        BookingException.requireOne(rows);
        boolean paid = target == OrderStatus.PAID;
        BookingSession session = new BookingSession();
        session.setSession_id(ticket.getSession_id());
        BookingException.requireOne(paid
                ? bookingMapper.dopaypriceUpdateSession(session)
                : bookingMapper.cancelSession(session));
        BookingException.requireOne(bookingCoreMapper.transitionSeat(
                ticket.getSession_id(),
                ticket.getSeat(),
                orderno,
                paid ? "SOLD" : "AVAILABLE"));
        return true;
    }

    private BookingException conflict() {
        return new BookingException("INVALID_ORDER_STATE", "訂單狀態已變更或已逾期", HttpStatus.CONFLICT);
    }
}
