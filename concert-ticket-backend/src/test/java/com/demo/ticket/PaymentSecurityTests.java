package com.demo.ticket;

import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Mapper.BookingCoreMapper;
import com.demo.ticket.Mapper.BookingMapper;
import com.demo.ticket.Service.Admin.SalesSettingsService;
import com.demo.ticket.Service.Booking.BookingOrderService;
import com.demo.ticket.Service.Booking.BookingPaymentScheduler;
import com.demo.ticket.Service.Booking.BookingServiceImpl;
import com.demo.ticket.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentSecurityTests {

    @Test
    void simulationIsDisabledByDefaultAndCannotMarkOrderPaid() {
        BookingOrderService orders = mock(BookingOrderService.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.hasKey("userData:jwt:access:session:member@example.com")).thenReturn(true);
        BookingServiceImpl service = new BookingServiceImpl(mock(BookingMapper.class), redis,
                mock(NotifierConsumer.class), mock(BookingPaymentScheduler.class), mock(BookingCoreMapper.class),
                orders, mock(SalesSettingsService.class));
        LoginUser user = new LoginUser("session", "member@example.com", Instant.now().plusSeconds(60), true);
        BookingException exception = assertThrows(BookingException.class, () -> service.dopayprice(null, user));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("PAYMENT_SIMULATION_DISABLED", exception.getCode());
        verifyNoInteractions(orders);
    }
}
