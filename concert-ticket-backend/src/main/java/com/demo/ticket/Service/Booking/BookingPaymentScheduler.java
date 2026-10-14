// 閱讀導引：依訂單付款期限安排提醒與逾期任務；付款或取消後可撤銷記憶體中的排程。
package com.demo.ticket.Service.Booking;

import com.demo.ticket.Config.WebSocket.NotificationMessage;
import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.bookingOrderStatus;
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
    private final BookingExpirationService expirationService;

    // 正式到期任務
    private final Map<String, ScheduledFuture<?>> expirationTasks = new ConcurrentHashMap<>();

    // 即將到期提醒任務
    private final Map<String, ScheduledFuture<?>> reminderTasks = new ConcurrentHashMap<>();

    // 到期前幾分鐘提醒
    private static final long REMINDER_MINUTES = 5;

    /**
     * 使用指定的 bookingTaskScheduler，並注入訂單查詢、逾期交易及通知元件。
     */
    public BookingPaymentScheduler(
            @Qualifier("bookingTaskScheduler")
            TaskScheduler taskScheduler,
            BookingMapper bookingMapper,
            NotifierConsumer notifier,
            BookingExpirationService expirationService
    ) {
        this.taskScheduler = taskScheduler;
        this.bookingMapper = bookingMapper;
        this.notifier = notifier;
        this.expirationService = expirationService;
    }

    /**
     * 在到期前 5 分鐘提醒，到期時呼叫逾期服務；已錯過提醒時間時只安排正式到期任務。
     * 排程存於記憶體，執行完會移除任務參照；重啟後的漏單由 BookingExpirationRecovery 補處理。
     * @param accessJwt 接收通知的會員 email，此參數並非 JWT 字串
     * @param bookingSaveTicket 已提交訂單，需包含訂單編號與付款期限
     */
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
     * 檢查訂單狀態後發送付款即將到期提醒。
     */
    private void remindExpiration(
            String accessJwt,
            BookingSaveTicket bookingSaveTicket
    ) {

        String status = bookingMapper.selectTicketStatus(bookingSaveTicket);

        // 已經付款、取消、過期，就不要通知
        if (!bookingOrderStatus.PENDING_PAYMENT.name().equals(status)) {
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
     * 執行逾期處理並在成功時發送期限已到通知。
     */
    private void expireTicket(
            String accessJwt,
            BookingSaveTicket bookingSaveTicket
    ) {
        if (!expirationService.expire(bookingSaveTicket)) {
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
    }

    /**
     * 以 JVM 預設時區將付款期限轉為 yyyy-MM-dd HH:mm:ss 通知文字。
     */
    private String dateFormat(Date date) {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss"
        ).format(date);
    }

    /**
     * 移除訂單的提醒及逾期任務，使用 cancel(false) 避免中斷已開始的工作；正在執行的任務仍須靠訂單狀態防止重複處理。
     */
    public void cancelExpiration(String orderno) {

        // 取消即將到期提醒
        ScheduledFuture<?> reminderFuture = reminderTasks.remove(orderno);
        if (reminderFuture != null) {
            reminderFuture.cancel(false);
        }

        // 取消正式到期
        ScheduledFuture<?> expirationFuture = expirationTasks.remove(orderno);
        if (expirationFuture != null) {
            expirationFuture.cancel(false);
        }
    }
}
