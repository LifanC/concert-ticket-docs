package com.demo.ticket.Service;

import com.demo.ticket.Dto.Booking.BookingOrder;
import com.demo.ticket.Dto.Booking.bookingOrderStatus;
import com.demo.ticket.Mapper.BookingCoreMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BookingExpirationRecovery {
    private static final Logger log = LoggerFactory.getLogger(BookingExpirationRecovery.class);
    private final BookingCoreMapper bookingCoreMapper;
    private final BookingOrderService bookingOrderService;

    public BookingExpirationRecovery(
            BookingCoreMapper bookingCoreMapper,
            BookingOrderService bookingOrderService
    ) {
        this.bookingCoreMapper = bookingCoreMapper;
        this.bookingOrderService = bookingOrderService;
    }

    @Scheduled(fixedDelayString = "${booking.expiration.scan-delay-ms:30000}")
    public void recover() {
        // Each order commits independently. Competing instances are safe through conditional updates.
        // 每個訂單獨立提交。透過條件更新，競爭實例是安全的。
        for (String orderno : bookingCoreMapper.expiredOrders()) {
            try {
                BookingOrder bookingOrder = new BookingOrder();
                bookingOrder.setOrderno(orderno);
                bookingOrder.setSession_id(null);
                bookingOrder.setEmail(null);
                bookingOrder.setStatus(bookingOrderStatus.EXPIRED);
                bookingOrderService.transition(bookingOrder);
            } catch (RuntimeException ex) {
                log.error("Expiration recovery failed for order {}", orderno, ex);
            }
        }
    }
}
