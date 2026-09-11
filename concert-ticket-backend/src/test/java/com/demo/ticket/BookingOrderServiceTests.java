package com.demo.ticket;

import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Mapper.BookingCoreMapper;
import com.demo.ticket.Mapper.BookingMapper;
import com.demo.ticket.Service.BookingOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.demo.ticket.Dto.Booking.bookingOrderStatus.*;

class BookingOrderServiceTests {
    BookingMapper mapper;
    BookingCoreMapper core;
    BookingOrderService service;
    BookingSaveTicket ticket;
    BookingOrder order;

    @BeforeEach
    void setup() {
        mapper = mock(BookingMapper.class);
        core = mock(BookingCoreMapper.class);
        service = new BookingOrderService(mapper, core);
        ticket = new BookingSaveTicket();
        ticket.setOrderno("TEST-1");
        ticket.setEmail("wang@user.com");
        ticket.setSession_id("S1");
        ticket.setSeat("A1");
        ticket.setStatus("PENDING_PAYMENT");
        order = new BookingOrder();
        order.setOrderno("TEST-1");
        order.setEmail("wang@user.com");
        order.setSession_id("S1");
        order.setStatus(PAID);
        when(core.findOrder("TEST-1")).thenReturn(ticket);
    }

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

    @ParameterizedTest
    @EnumSource(value = bookingOrderStatus.class, names = {"PAID", "CANCELLED", "EXPIRED", "REFUNDED"})
    void terminalOrdersCannotBePaidAgain(bookingOrderStatus current) {
        ticket.setStatus(current.name());
        assertEquals(HttpStatus.CONFLICT, assertThrows(BookingException.class,
                () -> service.transition(order)).getStatus());
        verifyNoInteractions(mapper);
        verify(core, never()).transitionSeat(any());
    }

    @Test
    void anotherMembersOrderIsHiddenBeforeLocking() {
        order.setEmail("other@example.test");
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BookingException.class,
                () -> service.transition(order)).getStatus());
        verify(core, never()).lockSession(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void wrongSessionIsHidden() {
        order.setSession_id("S2");
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BookingException.class,
                () -> service.transition(order)).getStatus());
        verifyNoInteractions(mapper);
    }

    @Test
    void repeatedExpirationDoesNotReleaseInventory() {
        ticket.setStatus("EXPIRED");
        order.setStatus(EXPIRED);
        assertFalse(service.transition(order));
        verifyNoInteractions(mapper);
        verify(core, never()).transitionSeat(any());
    }

    @Test
    void lostConditionalUpdateDoesNotChangeInventory() {
        when(mapper.dopaypriceTicket(any())).thenReturn(0);
        assertEquals(HttpStatus.CONFLICT, assertThrows(BookingException.class,
                () -> service.transition(order)).getStatus());
        verify(mapper, never()).dopaypriceUpdateSession(any());
        verify(core, never()).transitionSeat(any());
    }

    @Test
    void inventoryMismatchThrowsInsteadOfReportingSuccess() {
        when(mapper.dopaypriceTicket(any())).thenReturn(1);
        when(mapper.dopaypriceUpdateSession(any())).thenReturn(0);
        assertEquals("INVENTORY_CONSISTENCY_ERROR", assertThrows(BookingException.class,
                () -> service.transition(order)).getCode());
        verify(core, never()).transitionSeat(any());
    }

    @Test
    void stateIsReadAgainAfterObtainingLock() {
        BookingSaveTicket paid = new BookingSaveTicket();
        paid.setStatus("PAID");
        when(core.findOrder("TEST-1")).thenReturn(ticket, paid);
        assertThrows(BookingException.class, () -> service.transition(order));
        var sequence = inOrder(core);
        sequence.verify(core).findOrder("TEST-1");
        sequence.verify(core).lockSession("S1");
        sequence.verify(core).findOrder("TEST-1");
        verifyNoInteractions(mapper);
    }
}
