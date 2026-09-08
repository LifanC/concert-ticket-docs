package com.demo.ticket.Service;

import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Config.WebSocket.NotificationMessage;
import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.Mapper.BookingCoreMapper;
import com.demo.ticket.Mapper.BookingMapper;
import com.demo.ticket.security.LoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingMapper bookingMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotifierConsumer notifierConsumer;
    private final BookingPaymentScheduler bookingPaymentScheduler;
    private final BookingCoreMapper bookingCoreMapper;
    private final BookingOrderService bookingOrderService;

    public BookingServiceImpl(
            BookingMapper bookingMapper,
            StringRedisTemplate stringRedisTemplate,
            NotifierConsumer notifierConsumer,
            BookingPaymentScheduler bookingPaymentScheduler,
            BookingCoreMapper bookingCoreMapper,
            BookingOrderService bookingOrderService
    ) {
        this.bookingMapper = bookingMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.notifierConsumer = notifierConsumer;
        this.bookingPaymentScheduler = bookingPaymentScheduler;
        this.bookingCoreMapper = bookingCoreMapper;
        this.bookingOrderService = bookingOrderService;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyActivities(
            BookingSelectOnlyActivitiesRequest request, LoginUser user) {
        final String activity_id = request.activity_id().trim();
        final String session_id = request.session_id().trim();
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlyActivities(activity_id, session_id)
                : new ArrayList<>();
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlySession(BookingSelectOnlySessionRequest request, LoginUser user) {
        final String date = request.date().trim();
        final String activity_id = request.activity_id().trim();
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
        final String activity_id = request.activity_id().trim();
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlyActivitiesPrice(activity_id).get(activity_id)
                : new HashMap<>();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveTicket(BookingSaveTicketRequest request, LoginUser user, String idempotencyKey) {
        requireAuthenticated(user);
        final String session_id = request.session_id().trim();
        final String activity_id = request.activity_id().trim();
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new BookingException("INVALID_IDEMPOTENCY_KEY", "請提供有效的 Idempotency-Key", HttpStatus.BAD_REQUEST);
        }
        final String seat = request.seat().trim();
        final String hash = requestHash(session_id, activity_id, seat);
        if (bookingCoreMapper.claimKey(user.email(), idempotencyKey, hash) == 0) {
            Map<String, Object> previous = bookingCoreMapper.findKey(user.email(), idempotencyKey);
            if (!hash.equals(previous.get("request_hash"))) {
                throw new BookingException("IDEMPOTENCY_CONFLICT", "相同請求識別碼不能用於不同訂位內容", HttpStatus.CONFLICT);
            }
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(previous.get("response_body"));
        }
        bookingCoreMapper.lockSession(session_id);
        Map<String, Object> snapshot = bookingCoreMapper.sessionSnapshot(session_id);
        if (snapshot == null || !activity_id.equals(snapshot.get("activity_id"))) {
            throw new BookingException("SESSION_NOT_FOUND", "找不到活動場次", HttpStatus.NOT_FOUND);
        }
        final String name = snapshot.get("name").toString();
        final String date = snapshot.get("date").toString();
        final String time = snapshot.get("time").toString();
        if (snapshot.get("price") == null || new BigDecimal(snapshot.get("price").toString()).signum() < 0) {
            throw new BookingException("INVALID_TICKET_PRICE", "活動票價設定無效", HttpStatus.UNPROCESSABLE_CONTENT);
        }
        bookingCoreMapper.ensureSeat(session_id, seat);
        List<Map<String, Object>> data;
        final String accessJwt = user.email();
        BookingSession bookingSession = new BookingSession();
        bookingSession.setSession_id(session_id);
        int cnt = bookingMapper.updateSession(bookingSession);
        String createdOrderNo;
        if (cnt == 1) {
            BookingSaveTicket bookingSaveTicket = new BookingSaveTicket();
            // "訂單編號格式需為 CTYYYYMMDDNNN，例如 CT20260815001" SQL處理
            bookingSaveTicket.setSession_id(session_id);
            bookingSaveTicket.setEmail(accessJwt);
            bookingSaveTicket.setName(name);
            bookingSaveTicket.setDate(date);
            bookingSaveTicket.setTime(time);
            BigDecimal price = new BigDecimal(snapshot.get("price").toString());
            bookingSaveTicket.setStatus(OrderStatus.PENDING_PAYMENT.name());
            bookingSaveTicket.setSeat(seat);
            bookingSaveTicket.setPrice(price);
            // 可付款時間10分鐘
            int minutes = 10;
            Date dateNow = new Date();
            Date dateExpiresAt = Date.from(dateNow
                    .toInstant()
                    .plus(minutes, ChronoUnit.MINUTES)
            );
            bookingSaveTicket.setExpires_at(dateExpiresAt);
            String orderno = bookingMapper.saveTicket(bookingSaveTicket);
            createdOrderNo = orderno;
            if (orderno == null || orderno.isBlank()) {
                BookingException.requireOne(0);
            }
            bookingSaveTicket.setOrderno(orderno);
            if (bookingCoreMapper.reserveSeat(bookingSaveTicket) != 1) {
                throw new BookingException("SEAT_ALREADY_RESERVED", "座位不存在或已被保留", HttpStatus.CONFLICT);
            }
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

            // Transaction commit 成功後，安排 expires_at 時執行
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            bookingPaymentScheduler.scheduleExpiration(accessJwt, bookingSaveTicket);
                            notifierConsumer.sendNotification(message);
                        }
                    }
            );
        } else {
            throw new BookingException("INSUFFICIENT_CAPACITY", "場次不存在或已售完", HttpStatus.CONFLICT);
        }
        HttpStatus status = HttpStatus.CREATED;
        Object response = ApiResponse.api(status, data);
        try {
            String body = new ObjectMapper().writeValueAsString(response);
            BookingException.requireOne(
                    bookingCoreMapper.completeKey(user.email(), idempotencyKey, createdOrderNo, body)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot store booking result", ex);
        }
        return ResponseEntity.status(status).body(response);
    }

    private String dateFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    private String requestHash(String session, String activity, String seat) {
        try {
            byte[] content = new ObjectMapper().writeValueAsBytes(
                    List.of(session, activity, seat)
            );
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to generate request hash", ex);
        }
    }

    private void requireAuthenticated(LoginUser user) {
        boolean invalid = user == null
                || !Boolean.TRUE.equals(user.accessExists())
                || !Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                        String.format(RedisKey.redisKey.get("access"), user.tokenId(), user.email())))
                || Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                        String.format(RedisKey.redisKey.get("blacklist"), user.tokenId())
        ));
        if (invalid) {
            throw new BookingException("UNAUTHORIZED", "請重新登入", HttpStatus.UNAUTHORIZED);
        }
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { action.run(); }
        });
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> cancelOrder(BookingCanceTicketRequest request, LoginUser user) {
        requireAuthenticated(user);
        String orderno = request.orderno().trim();
        bookingOrderService.transition(orderno, request.session_id().trim(), user.email(), OrderStatus.CANCELLED);
        afterCommit(() -> {
            bookingPaymentScheduler.cancelExpiration(orderno);
            notifierConsumer.sendNotification(
                    new NotificationMessage(
                            user.email(),
                            "取消成功",
                            "請至訂單頁面查看票券資訊"
                    )
            );
        });
        return ResponseEntity.ok(ApiResponse.api(HttpStatus.OK, List.of(Map.of("judge", true))));
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public Map<String, Object> sessionSalesDate(BookingSessionSalesDateRequest request, LoginUser user) {
        final String session_id = request.session_id().trim();
        final String activity_id = request.activity_id().trim();
        final String ticket_status = request.status().trim();
        final String date = request.date().trim();
        final String time = request.time().trim();
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
        requireAuthenticated(user);
        String orderno = request.orderno().trim();
        bookingOrderService.transition(orderno, request.session_id().trim(), user.email(), OrderStatus.PAID);
        afterCommit(() -> {
            bookingPaymentScheduler.cancelExpiration(orderno);
            notifierConsumer.sendNotification(
                    new NotificationMessage(
                            user.email(),
                            "付款成功",
                            "請至訂單頁面查看票券資訊"
                    )
            );
        });
        return ResponseEntity.ok(ApiResponse.api(HttpStatus.OK, List.of(Map.of("judge", true))));
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlySeats(BookingSelectOnlySeatsRequest request, LoginUser user) {
        final String activityId = request.activity_id().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        if (Boolean.TRUE.equals(user.accessExists())) {
            Map<String, Object> seatConfig = bookingMapper.selectOnlySeats(activityId).get(activityId);
            if (seatConfig != null) {
                String[] rows = seatConfig.get("seat_rows").toString().split(",");
                int seatsPerRow = Integer.parseInt(seatConfig.get("seats_per_row").toString());
                for (String row : rows) {
                    for (int number = 1; number <= seatsPerRow; number++) {
                        data.add(Map.of(
                                "id", "%s-%02d".formatted(row, number),
                                "row", row,
                                "number", number,
                                "seats_per_row", seatsPerRow
                        ));
                    }
                }
            }
        }
        return data;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<String> selectOnlyUnavailableSeats(BookingSelectOnlyUnavailableSeatsRequest request, LoginUser user) {
        requireAuthenticated(user);
        return bookingCoreMapper.unavailableSeats(request.session_id().trim());
    }

}
