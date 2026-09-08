package com.demo.ticket.Service;

import com.demo.ticket.Dto.Booking.OrderStatus;
import com.demo.ticket.Mapper.BookingCoreMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BookingExpirationRecovery {
    private static final Logger log = LoggerFactory.getLogger(BookingExpirationRecovery.class);
    private final BookingCoreMapper core;
    private final BookingOrderService orders;

    public BookingExpirationRecovery(BookingCoreMapper core, BookingOrderService orders) {
        this.core = core;
        this.orders = orders;
    }

    @Scheduled(fixedDelayString = "${booking.expiration.scan-delay-ms:30000}")
    public void recover() {
        // Each order commits independently. Competing instances are safe through conditional updates.
        for (String orderno : core.expiredOrders()) {
            try {
                orders.transition(orderno, null, null, OrderStatus.EXPIRED);
            } catch (RuntimeException ex) {
                log.error("Expiration recovery failed for order {}", orderno, ex);
            }
        }
    }
}
