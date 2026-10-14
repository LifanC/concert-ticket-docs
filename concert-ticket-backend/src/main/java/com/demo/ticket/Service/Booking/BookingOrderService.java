// 閱讀導引：統一處理付款、取消與逾期，讓訂單、場次保留數及座位在同一交易內轉換。
package com.demo.ticket.Service.Booking;

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

    /**
     * 注入訂單／庫存與座位的更新介面，供單一交易同步三種資料。
     */
    public BookingOrderService(
            BookingMapper bookingMapper,
            BookingCoreMapper bookingCoreMapper
    ) {
        this.bookingMapper = bookingMapper;
        this.bookingCoreMapper = bookingCoreMapper;
    }

    /**
     * 鎖定場次後重新讀取訂單，再以條件更新同步訂單、庫存與座位。
     * 所有更新必須恰好影響一筆資料；任一步異常就拋出例外，讓整筆交易回滾。
     *
     * @param bookingOrder 訂單編號、目標狀態與可選的 email、場次核對條件；null 核對條件會略過
     * @return 成功轉換為 true；逾期任務遇到不存在、已處理或未到期訂單時為 false
     * @throws BookingException 非逾期操作找不到訂單、狀態衝突或更新筆數異常
     */
    @Transactional
    public boolean transition(BookingOrder bookingOrder) {
        final String orderno = bookingOrder.getOrderno();
        final String email = bookingOrder.getEmail();
        final String sessionId = bookingOrder.getSession_id();
        final bookingOrderStatus target = bookingOrder.getStatus();
        BookingSaveTicket ticket = bookingCoreMapper.findOrder(orderno);
        boolean notFound = ticket == null
                || (email != null && !email.equals(ticket.getEmail()))
                || (sessionId != null && !sessionId.equals(ticket.getSession_id()));
        if (notFound) {
            if (target == bookingOrderStatus.EXPIRED) {
                return false;
            }
            throw new BookingException("ORDER_NOT_FOUND", "找不到訂單", HttpStatus.NOT_FOUND);
        }
        // 與訂票流程使用相同鎖定順序：先鎖場次，再更新訂單與座位，降低交錯鎖定造成死結的機會。
        bookingCoreMapper.lockSession(ticket.getSession_id());
        // 取得場次鎖後重新讀取，避免使用等待鎖之前的舊訂單狀態。
        ticket = bookingCoreMapper.findOrder(orderno);
        bookingOrderStatus current = bookingOrderStatus.valueOf(ticket.getStatus());
        // 逾期背景任務遇到已處理訂單可直接略過；使用者的非法轉換則回傳衝突。
        if (!current.canTransitionTo(target)) {
            if (target == bookingOrderStatus.EXPIRED) {
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
            // 狀態列舉保留 REFUNDED，但尚無退款、庫存與金額的完整處理流程，因此此處拒絕退款。
            default -> throw conflict();
        }
        if (rows == 0) {
            if (target == bookingOrderStatus.EXPIRED) {
                return false;
            }
            throw conflict();
        }
        BookingException.requireOne(rows);
        // 付款將保留轉成售出；取消或逾期則歸還庫存，並將座位恢復可選。
        boolean paid = target == bookingOrderStatus.PAID;
        BookingSession session = new BookingSession();
        session.setSession_id(ticket.getSession_id());
        BookingException.requireOne(paid
                ? bookingMapper.dopaypriceUpdateSession(session)
                : bookingMapper.cancelSession(session));
        BookingTransitionSeat bookingTransitionSeat = new  BookingTransitionSeat();
        bookingTransitionSeat.setSessionId(ticket.getSession_id());
        bookingTransitionSeat.setSeat(ticket.getSeat());
        bookingTransitionSeat.setOrderno(orderno);
        bookingTransitionSeat.setStatus(paid ? "SOLD" : "AVAILABLE");
        BookingException.requireOne(bookingCoreMapper.transitionSeat(bookingTransitionSeat));
        return true;
    }

    /**
     * 建立訂單狀態衝突例外，讓呼叫端回傳 HTTP 409。
     */
    private BookingException conflict() {
        return new BookingException("INVALID_ORDER_STATE", "訂單狀態已變更或已逾期", HttpStatus.CONFLICT);
    }
}
