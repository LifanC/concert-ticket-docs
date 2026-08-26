package com.demo.ticket.Service;

import com.demo.ticket.Config.WebSocket.NotificationMessage;
import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Mapper.BookingMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class BookingPaymentScheduler {

    private final TaskScheduler taskScheduler;
    private final BookingMapper bookingMapper;
    private final NotifierConsumer notifier;

    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public BookingPaymentScheduler(
            @Qualifier("bookingTaskScheduler")
            TaskScheduler taskScheduler,
            BookingMapper bookingMapper,
            NotifierConsumer notifier
    ) {
        this.taskScheduler = taskScheduler;
        this.bookingMapper = bookingMapper;
        this.notifier = notifier;
    }

    public void scheduleExpiration(
            String accessJwt,
            BookingSaveTicket bookingSaveTicket) {
        String orderno = bookingSaveTicket.getOrderno();
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    try {
                        expireTicket(accessJwt, bookingSaveTicket);
                    } finally {
                        // 任務執行完就移除
                        tasks.remove(orderno);
                    }
                },
                bookingSaveTicket.getExpires_at().toInstant()
        );
        tasks.put(orderno, future);
    }

    private void expireTicket(String accessJwt, BookingSaveTicket bookingSaveTicket) {
        String status = bookingMapper.selectTicketStatus(bookingSaveTicket);
        if (!"PENDING_PAYMENT".equals(status)) {
            return;
        }
        NotificationMessage message =
                new NotificationMessage(
                        accessJwt,
                        "付款期限已到",
                        bookingSaveTicket.getOrderno() + " 的訂單因逾期未付款已失效" +
                                " 的付款時間已截止，付款期限：" +
                                dateFormat(bookingSaveTicket.getExpires_at())

                );
        notifier.sendNotification(message);
        bookingSaveTicket.setStatus("EXPIRED");
        bookingSaveTicket.setCancelled_at(bookingSaveTicket.getExpires_at());
        bookingMapper.updateTicketExpiredAt(bookingSaveTicket);
    }

    private String dateFormat(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    public boolean cancelExpiration(String orderno) {
        ScheduledFuture<?> future = tasks.remove(orderno);
        if (future == null) {
            return false;
        }
        return future.cancel(false);
    }

}
