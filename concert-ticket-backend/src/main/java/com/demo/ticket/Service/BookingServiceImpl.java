package com.demo.ticket.Service;

import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Config.WebSocket.NotificationMessage;
import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.Mapper.BookingMapper;
import com.demo.ticket.security.LoginUser;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingMapper bookingMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotifierConsumer notifier;
    private final BookingPaymentScheduler bookingPaymentScheduler;

    public BookingServiceImpl(
            BookingMapper bookingMapper,
            StringRedisTemplate stringRedisTemplate,
            NotifierConsumer notifier,
            BookingPaymentScheduler bookingPaymentScheduler
    ) {
        this.bookingMapper = bookingMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.notifier = notifier;
        this.bookingPaymentScheduler = bookingPaymentScheduler;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyActivities(
            BookingSelectOnlyActivitiesRequest request, LoginUser user) {
        final String activity_id = request.getActivity_id().trim();
        final String session_id = request.getSession_id().trim();
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlyActivities(activity_id, session_id)
                : new ArrayList<>();
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlySession(BookingSelectOnlySessionRequest request, LoginUser user) {
        final String date = request.getDate().trim();
        final String activity_id = request.getActivity_id().trim();
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlySession(date, activity_id)
                : new ArrayList<>();
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyTicket(LoginUser user) {
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlyTicket(user.email())
                : new ArrayList<>();
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public Map<String, Object> selectOnlyActivitiesPrice(
            BookingSelectOnlyActivitiesPriceRequest request, LoginUser user) {
        final String activity_id = request.getActivity_id().trim();
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlyActivitiesPrice(activity_id).get(activity_id)
                : new HashMap<>();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveTicket(BookingSaveTicketRequest request, LoginUser user) {
        final String session_id = request.getSession_id().trim();
        final String activity_id = request.getActivity_id().trim();
        final String name = request.getName().trim();
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        final String ticket_status = request.getStatus().trim();
        final String seat = request.getSeat().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        final String accessJtId = user.tokenId();
        final String accessJwt = user.email();
        if (Boolean.TRUE.equals(user.accessExists())) {
            final String blacklistRedisKey = String.format(
                    RedisKey.redisKey.get("blacklist"),
                    accessJtId
            );
            Boolean blacklistExists = stringRedisTemplate.hasKey(blacklistRedisKey);
            if (Boolean.FALSE.equals(blacklistExists)) {
                BookingSession bookingSession = new BookingSession();
                bookingSession.setSession_id(session_id);
                int cnt = bookingMapper.updateSession(bookingSession);
                if (cnt > 0) {
                    BookingSaveTicket bookingSaveTicket = new BookingSaveTicket();
                    // "訂單編號格式需為 CTYYYYMMDDNNN，例如 CT20260815001" SQL處理
                    bookingSaveTicket.setSession_id(session_id);
                    bookingSaveTicket.setEmail(accessJwt);
                    bookingSaveTicket.setName(name);
                    bookingSaveTicket.setDate(date);
                    bookingSaveTicket.setTime(time);
                    BigDecimal price = bookingMapper.selectActivityPrice(activity_id);
                    bookingSaveTicket.setStatus(ticket_status);
                    bookingSaveTicket.setSeat(seat);
                    bookingSaveTicket.setPrice(price == null ? BigDecimal.ZERO : price);
                    // 可付款時間10分鐘
                    int minutes = 10;
                    Date dateNow = new Date();
                    Date dateExpiresAt = Date.from(dateNow
                            .toInstant()
                            .plus(minutes, ChronoUnit.MINUTES)
                    );
                    bookingSaveTicket.setExpires_at(dateExpiresAt);
                    String orderno = bookingMapper.saveTicket(bookingSaveTicket);
                    bookingSaveTicket.setOrderno(orderno);
                    data = bookingMapper.selectOnlyTicket(accessJwt);

                    Map<String, Object> sessionData = bookingMapper.selectOnlySessionId(session_id).get(session_id);
                    BigDecimal capacity = new BigDecimal(sessionData.get("capacity").toString());
                    NotificationMessage message =
                            new NotificationMessage(
                                    accessJwt,
                                    "新通知：請在 " + minutes + " 分鐘內完成付款，剩餘庫存：" + capacity,
                                    "尚未付款，付款期限：" +
                                            dateFormat(dateNow) + " ～ " + dateFormat(dateExpiresAt)
                            );
                    notifier.sendNotification(message);

                    // Transaction commit 成功後，安排 expires_at 時執行
                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    bookingPaymentScheduler.scheduleExpiration(accessJwt, bookingSaveTicket);
                                }
                            }
                    );
                } else {
                    NotificationMessage message =
                            new NotificationMessage(
                                    accessJwt,
                                    "新通知",
                                    name + "的庫存低於安全庫存量"
                            );
                    notifier.sendNotification(message);
                }
            }
        }
        HttpStatus status = HttpStatus.CREATED;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    private String dateFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> cancelOrder(BookingCanceTicketRequest request, LoginUser user) {
        final String orderno = request.getOrderno().trim();
        final String session_id = request.getSession_id().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        final String accessJtId = user.tokenId();
        final String accessJwt = user.email();
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("judge", false);
        if (Boolean.TRUE.equals(user.accessExists())) {
            final String blacklistRedisKey = String.format(
                    RedisKey.redisKey.get("blacklist"),
                    accessJtId
            );
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(blacklistRedisKey))) {
                BookingSession bookingSession = new BookingSession();
                bookingSession.setSession_id(session_id);
                int cntUpdateSession = bookingMapper.cancelSession(bookingSession);
                if (cntUpdateSession > 0) {
                    BookingSaveTicket bookingSaveTicket = new BookingSaveTicket();
                    bookingSaveTicket.setOrderno(orderno);
                    int cntCancelTicket = bookingMapper.cancelTicket(bookingSaveTicket);
                    if (cntCancelTicket > 0) {
                        dataMap.put("judge", true);
                        NotificationMessage message =
                                new NotificationMessage(
                                        accessJwt,
                                        "取消訂單",
                                        "您的票券已取消成功，可至訂單頁面查看票券資訊"
                                );
                        notifier.sendNotification(message);
                    }

                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    bookingPaymentScheduler.cancelExpiration(orderno);
                                }
                            }
                    );

                    data.add(dataMap);
                }
            }
        }
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public Map<String, Object> sessionSalesDate(BookingSessionSalesDateRequest request, LoginUser user) {
        final String session_id = request.getSession_id().trim();
        final String activity_id = request.getActivity_id().trim();
        final String ticket_status = request.getStatus().trim();
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        Map<String, Object> dataMap = new HashMap<>();
        final String accessJtId = user.tokenId();
        if (Boolean.TRUE.equals(user.accessExists())) {
            final String blacklistRedisKey = String.format(
                    RedisKey.redisKey.get("blacklist"),
                    accessJtId
            );
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(blacklistRedisKey))) {
                if ("PENDING_PAYMENT".equals(ticket_status)) {
                    BookingSalesDate bookingSalesDate = new BookingSalesDate();
                    bookingSalesDate.setSession_id(session_id);
                    bookingSalesDate.setActivity_id(activity_id);
                    bookingSalesDate.setDate(date);
                    bookingSalesDate.setTime(time);
                    dataMap = bookingMapper.sessionSalesDate(bookingSalesDate).get(session_id);
                }
            }
        }
        return dataMap;

    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> dopayprice(BookingDopaypriceRequest request, LoginUser user) {
        final String orderno = request.getOrderno().trim();
        final String session_id = request.getSession_id().trim();
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        final String accessJwt = user.email();
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("judge", false);
        if (Boolean.TRUE.equals(user.accessExists())) {
            BookingSession bookingSession = new BookingSession();
            bookingSession.setSession_id(session_id);
            int cntUpdateSession = bookingMapper.dopaypriceUpdateSession(bookingSession);
            if (cntUpdateSession > 0) {
                BookingDopaypriceTicket bookingDopaypriceTicket = new BookingDopaypriceTicket();
                bookingDopaypriceTicket.setOrderno(orderno);
                bookingDopaypriceTicket.setSession_id(session_id);
                bookingDopaypriceTicket.setCustomer(accessJwt.substring(0, accessJwt.indexOf('@')));
                bookingDopaypriceTicket.setDate(date);
                bookingDopaypriceTicket.setTime(time);
                int cntDopaypriceTicket = bookingMapper.dopaypriceTicket(bookingDopaypriceTicket);
                if (cntDopaypriceTicket > 0) {
                    dataMap.put("judge", true);

                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    bookingPaymentScheduler.cancelExpiration(orderno);
                                }
                            }
                    );

                    NotificationMessage message =
                            new NotificationMessage(
                                    accessJwt,
                                    "付款成功",
                                    "您的票券已付款成功，可至訂單頁面查看票券資訊"
                            );
                    notifier.sendNotification(message);
                }
                data.add(dataMap);
            }
        }
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlySeats(BookingSelectOnlySeatsRequest request, LoginUser user) {
        final String activity_id = request.getActivity_id().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        if (Boolean.TRUE.equals(user.accessExists())) {
            Map<String, Object> dataMapOnlySeats = bookingMapper.selectOnlySeats(activity_id).get(activity_id);
            if (dataMapOnlySeats != null) {
                String seat_rows = dataMapOnlySeats.get("seat_rows").toString();
                String[] strings = seat_rows.split(",");
                int seats_per_row = Integer.parseInt(dataMapOnlySeats.get("seats_per_row").toString());
                for (String string : strings) {
                    for (int i = 0; i < seats_per_row; i++) {
                        final int number = i + 1;
                        Map<String, Object> dataMap = new HashMap<>();
                        dataMap.put("id", string + "-" + String.format("%02d", number));
                        dataMap.put("row", string);
                        dataMap.put("number", number);
                        dataMap.put("seats_per_row", seats_per_row);
                        data.add(dataMap);
                    }
                }
            }
        }
        return data;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<String> selectOnlyUnavailableSeats(BookingSelectOnlyUnavailableSeatsRequest request, LoginUser user) {
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        List<String> data = new ArrayList<>();
        if (Boolean.TRUE.equals(user.accessExists())) {
            BookingSaveTicket bookingSaveTicket = new BookingSaveTicket();
            bookingSaveTicket.setDate(date);
            bookingSaveTicket.setTime(time);
            List<Map<String, Object>> unavailableSeats = bookingMapper.selectOnlyUnavailableSeats(bookingSaveTicket);
            if (!unavailableSeats.isEmpty()) {
                unavailableSeats.forEach(unavailableSeat -> {
                    data.add(unavailableSeat.get("seat").toString());
                });
            }
        }
        return data;
    }
}
