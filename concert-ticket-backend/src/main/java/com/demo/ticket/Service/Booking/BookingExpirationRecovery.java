// 閱讀導引：定期從資料庫找出逾期訂單，補處理因服務重啟或排程遺漏而未釋放的座位。
package com.demo.ticket.Service.Booking;

import lombok.extern.slf4j.Slf4j;
import com.demo.ticket.Dto.Booking.BookingOrder;
import com.demo.ticket.Dto.Booking.bookingOrderStatus;
import com.demo.ticket.Mapper.BookingCoreMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BookingExpirationRecovery {
    private final BookingCoreMapper bookingCoreMapper;
    private final BookingOrderService bookingOrderService;

    /**
     * 注入逾期訂單查詢與狀態轉換服務，供定期補償掃描使用。
     */
    public BookingExpirationRecovery(
            BookingCoreMapper bookingCoreMapper,
            BookingOrderService bookingOrderService
    ) {
        this.bookingCoreMapper = bookingCoreMapper;
        this.bookingOrderService = bookingOrderService;
    }

    /**
     * 每輪最多取得 100 筆逾期待付款訂單，逐筆呼叫有交易的狀態轉換；某筆失敗只記錄錯誤，不中止其他訂單。
     */
    @Scheduled(fixedDelayString = "${booking.expiration.scan-delay-ms:30000}")
    public void recover() {
        // 每筆訂單各自提交；場次鎖與條件更新讓多個服務實例掃到同一訂單時，不會重複釋放庫存。
        for (String orderno : bookingCoreMapper.expiredOrders()) {
            try {
                BookingOrder bookingOrder = BookingOrder.builder()
                        .orderno(orderno)
                        .session_id(null)
                        .email(null)
                        .status(bookingOrderStatus.EXPIRED)
                        .build();
                bookingOrderService.transition(bookingOrder);
            } catch (RuntimeException ex) {
                log.error("Expiration recovery failed for order {}", orderno, ex);
            }
        }
    }
}
