// 閱讀導引：驗證訂單狀態轉換及資料更新的結果，防止付款、取消與逾期流程破壞一致性。
package com.demo.ticket;

import com.demo.ticket.Dto.Booking.BookingOrder;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.bookingOrderStatus;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Mapper.BookingCoreMapper;
import com.demo.ticket.Mapper.BookingMapper;
import com.demo.ticket.Service.Booking.BookingOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;

import static com.demo.ticket.Dto.Booking.bookingOrderStatus.EXPIRED;
import static com.demo.ticket.Dto.Booking.bookingOrderStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookingOrderServiceTests {

    BookingMapper mapper;
    BookingCoreMapper core;
    BookingOrderService service;
    BookingSaveTicket ticket;
    BookingOrder order;

    /**
     * 為每個測試初始化模擬元件與共用資料，避免測試情境互相影響。
     */
    @BeforeEach
    void setup() {
        mapper = mock(BookingMapper.class);
        core = mock(BookingCoreMapper.class);
        service = new BookingOrderService(mapper, core);
        ticket = BookingSaveTicket.builder()
                .orderno("TEST-1")
                .email("wang@user.com")
                .session_id("S1")
                .seat("A1")
                .status("PENDING_PAYMENT")
                .build();
        order = BookingOrder.builder()
                .orderno("TEST-1")
                .email("wang@user.com")
                .session_id("S1")
                .status(PAID)
                .build();
        when(core.findOrder("TEST-1")).thenReturn(ticket);
    }

    /**
     * 驗證待付款訂單轉換時恰好更新一次訂單、庫存與座位。
     */
    @ParameterizedTest
    @EnumSource(value = bookingOrderStatus.class, names = {"PAID", "CANCELLED", "EXPIRED"})
    void pendingOrderUpdatesInventoryAndSeatExactlyOnce(bookingOrderStatus target) {
        order.setStatus(target);
        when(mapper.dopaypriceTicket(any())).thenReturn(1);
        when(mapper.cancelTicket(any())).thenReturn(1);
        when(mapper.updateTicketExpiredAt(any())).thenReturn(1);
        when(mapper.dopaypriceUpdateSession(any())).thenReturn(1);
        when(mapper.cancelSession(any())).thenReturn(1);
        when(core.transitionSeat(any())).thenReturn(1);
        assertTrue(service.transition(order));
        verify(core).transitionSeat(argThat(seat ->
                seat.getOrderno().equals("TEST-1") && seat.getSessionId().equals("S1")
                        && seat.getSeat().equals("A1")
                        && seat.getStatus().equals(target == PAID ? "SOLD" : "AVAILABLE")));
        if (target == PAID) {
            verify(mapper).dopaypriceUpdateSession(any());
            verify(mapper, never()).cancelSession(any());
        } else {
            verify(mapper).cancelSession(any());
            verify(mapper, never()).dopaypriceUpdateSession(any());
        }
    }

    /**
     * 驗證終止狀態訂單不能再次付款。
     */
    @ParameterizedTest
    @EnumSource(value = bookingOrderStatus.class, names = {"PAID", "CANCELLED", "EXPIRED", "REFUNDED"})
    void terminalOrdersCannotBePaidAgain(bookingOrderStatus current) {
        ticket.setStatus(current.name());
        assertEquals(HttpStatus.CONFLICT, assertThrows(BookingException.class,
                () -> service.transition(order)).getStatus());
        verifyNoInteractions(mapper);
        verify(core, never()).transitionSeat(any());
    }

    /**
     * 驗證其他會員的訂單在取得鎖之前即被視為不存在。
     */
    @Test
    void anotherMembersOrderIsHiddenBeforeLocking() {
        order.setEmail("other@example.test");
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BookingException.class,
                () -> service.transition(order)).getStatus());
        verify(core, never()).lockSession(any());
        verifyNoInteractions(mapper);
    }

    /**
     * 驗證場次不符的訂單查詢被視為不存在。
     */
    @Test
    void wrongSessionIsHidden() {
        order.setSession_id("S2");
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BookingException.class,
                () -> service.transition(order)).getStatus());
        verifyNoInteractions(mapper);
    }

    /**
     * 驗證重複逾期處理不會再次歸還庫存。
     */
    @Test
    void repeatedExpirationDoesNotReleaseInventory() {
        ticket.setStatus("EXPIRED");
        order.setStatus(EXPIRED);
        assertFalse(service.transition(order));
        verifyNoInteractions(mapper);
        verify(core, never()).transitionSeat(any());
    }

    /**
     * 驗證訂單條件更新失敗時不再更新庫存或座位。
     */
    @Test
    void lostConditionalUpdateDoesNotChangeInventory() {
        when(mapper.dopaypriceTicket(any())).thenReturn(0);
        assertEquals(HttpStatus.CONFLICT, assertThrows(BookingException.class,
                () -> service.transition(order)).getStatus());
        verify(mapper, never()).dopaypriceUpdateSession(any());
        verify(core, never()).transitionSeat(any());
    }

    /**
     * 驗證庫存更新筆數異常時拋出例外，避免回報成功。
     */
    @Test
    void inventoryMismatchThrowsInsteadOfReportingSuccess() {
        when(mapper.dopaypriceTicket(any())).thenReturn(1);
        when(mapper.dopaypriceUpdateSession(any())).thenReturn(0);
        assertEquals("INVENTORY_CONSISTENCY_ERROR", assertThrows(BookingException.class,
                () -> service.transition(order)).getCode());
        verify(core, never()).transitionSeat(any());
    }

    /**
     * 驗證取得場次鎖後重新讀取訂單狀態，避免使用舊資料。
     */
    @Test
    void stateIsReadAgainAfterObtainingLock() {
        BookingSaveTicket paid = BookingSaveTicket.builder()
                .status("PAID")
                .build();
        when(core.findOrder("TEST-1")).thenReturn(ticket, paid);
        assertThrows(BookingException.class, () -> service.transition(order));
        InOrder sequence = inOrder(core);
        sequence.verify(core).findOrder("TEST-1");
        sequence.verify(core).lockSession("S1");
        sequence.verify(core).findOrder("TEST-1");
        verifyNoInteractions(mapper);
    }
}
