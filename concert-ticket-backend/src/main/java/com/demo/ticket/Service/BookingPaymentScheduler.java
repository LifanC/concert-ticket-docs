package com.demo.ticket.Service;

import com.demo.ticket.Config.WebSocket.NotificationMessage;
import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.BookingSession;
import com.demo.ticket.Mapper.BookingMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class BookingPaymentScheduler {

    private final TaskScheduler taskScheduler;
    private final BookingMapper bookingMapper;
    private final NotifierConsumer notifier;

    // 正式到期任務
    private final Map<String, ScheduledFuture<?>> expirationTasks = new ConcurrentHashMap<>();

    // 即將到期提醒任務
    private final Map<String, ScheduledFuture<?>> reminderTasks = new ConcurrentHashMap<>();

    // 到期前幾分鐘提醒
    private static final long REMINDER_MINUTES = 5;

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
            BookingSaveTicket bookingSaveTicket
    ) {

        String orderno = bookingSaveTicket.getOrderno();

        /*
         * 1. 排程「即將到期」提醒
         */
        Instant reminderTime = bookingSaveTicket
                .getExpires_at()
                .toInstant()
                .minusSeconds(REMINDER_MINUTES * 60);

        // 避免提醒時間已經過了還去排程
        if (reminderTime.isAfter(Instant.now())) {

            ScheduledFuture<?> reminderFuture =
                    taskScheduler.schedule(
                            () -> {
                                try {
                                    remindExpiration(
                                            accessJwt,
                                            bookingSaveTicket
                                    );
                                } finally {
                                    reminderTasks.remove(orderno);
                                }
                            },
                            reminderTime
                    );

            reminderTasks.put(orderno, reminderFuture);
        }

        /*
         * 2. 排程「正式到期」
         */
        ScheduledFuture<?> expirationFuture =
                taskScheduler.schedule(
                        () -> {
                            try {
                                expireTicket(
                                        accessJwt,
                                        bookingSaveTicket
                                );
                            } finally {
                                expirationTasks.remove(orderno);
                            }
                        },
                        bookingSaveTicket
                                .getExpires_at()
                                .toInstant()
                );

        expirationTasks.put(orderno, expirationFuture);
    }

    /**
     * 即將到期提醒
     */
    private void remindExpiration(
            String accessJwt,
            BookingSaveTicket bookingSaveTicket
    ) {

        String status = bookingMapper.selectTicketStatus(bookingSaveTicket);

        // 已經付款、取消、過期，就不要通知
        if (!"PENDING_PAYMENT".equals(status)) {
            return;
        }

        NotificationMessage message =
                new NotificationMessage(
                        accessJwt,
                        "付款即將到期",
                        bookingSaveTicket.getOrderno()
                                + " 的訂單付款期限即將到期，"
                                + "請儘快完成付款。付款期限："
                                + dateFormat(bookingSaveTicket.getExpires_at())
                );

        notifier.sendNotification(message);
    }

    /**
     * 正式到期
     */
    private void expireTicket(
            String accessJwt,
            BookingSaveTicket bookingSaveTicket
    ) {

        String status =
                bookingMapper.selectTicketStatus(bookingSaveTicket);

        if (!"PENDING_PAYMENT".equals(status)) {
            return;
        }

        NotificationMessage message =
                new NotificationMessage(
                        accessJwt,
                        "付款期限已到",
                        bookingSaveTicket.getOrderno()
                                + " 的訂單因逾期未付款已失效，"
                                + "付款期限："
                                + dateFormat(bookingSaveTicket.getExpires_at())
                );

        notifier.sendNotification(message);

        bookingSaveTicket.setCancelled_at(bookingSaveTicket.getExpires_at());
        bookingMapper.updateTicketExpiredAt(bookingSaveTicket);
        BookingSession bookingSession = new BookingSession();
        bookingSession.setSession_id(bookingSaveTicket.getSession_id());
        bookingMapper.cancelSession( bookingSession);
    }

    private String dateFormat(Date date) {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss"
        ).format(date);
    }

    /**
     * 付款成功 / 訂單取消時
     * 同時取消提醒 + 到期任務
     */
    public boolean cancelExpiration(String orderno) {

        boolean cancelled = false;

        // 取消即將到期提醒
        ScheduledFuture<?> reminderFuture =reminderTasks.remove(orderno);

        if (reminderFuture != null) {
            reminderFuture.cancel(false);
            cancelled = true;
        }

        // 取消正式到期
        ScheduledFuture<?> expirationFuture =expirationTasks.remove(orderno);

        if (expirationFuture != null) {
            expirationFuture.cancel(false);
            cancelled = true;
        }

        return cancelled;
    }
}