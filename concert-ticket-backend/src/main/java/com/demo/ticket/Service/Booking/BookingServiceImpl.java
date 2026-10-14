// 閱讀導引：訂票流程：驗證登入、取得場次與票價、保留座位並建立待付款訂單；通知與排程在交易提交後執行。
package com.demo.ticket.Service.Booking;

import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Dto.Admin.Quote;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Config.WebSocket.NotificationMessage;
import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.Mapper.BookingCoreMapper;
import com.demo.ticket.Mapper.BookingMapper;
import com.demo.ticket.Service.Admin.SalesSettingsService;
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
    private final SalesSettingsService salesSettingsService;

    /**
     * 注入訂票資料存取、Redis 登入檢查、計價、訂單轉換與付款通知排程等元件。
     */
    public BookingServiceImpl(
            BookingMapper bookingMapper,
            StringRedisTemplate stringRedisTemplate,
            NotifierConsumer notifierConsumer,
            BookingPaymentScheduler bookingPaymentScheduler,
            BookingCoreMapper bookingCoreMapper,
            BookingOrderService bookingOrderService,
            SalesSettingsService salesSettingsService
    ) {
        this.bookingMapper = bookingMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.notifierConsumer = notifierConsumer;
        this.bookingPaymentScheduler = bookingPaymentScheduler;
        this.bookingCoreMapper = bookingCoreMapper;
        this.bookingOrderService = bookingOrderService;
        this.salesSettingsService = salesSettingsService;
    }

    /**
     * 查詢指定活動有場次的演出日期，回傳下拉選單使用的 value、label 與 note；登入紀錄不存在時回傳空清單。
     */
    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyActivities(
            BookingSelectOnlyActivitiesRequest request, LoginUser user) {
        final String activity_id = request.activity_id().trim();
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlyActivities(activity_id)
                : new ArrayList<>();
    }

    /**
     * 依活動與演出日期查詢場次時間及剩餘可售數；查詢本身不排除售完或尚未開賣的場次。
     */
    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlySession(BookingSelectOnlySessionRequest request, LoginUser user) {
        final String date = request.date().trim();
        final String activity_id = request.activity_id().trim();
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlySession(date, activity_id)
                : new ArrayList<>();
    }

    /**
     * 以登入會員的 email 查詢票券，回傳訂單、座位、票價及狀態；不接受前端指定其他會員。
     */
    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyTicket(LoginUser user) {
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlyTicket(user.email())
                : new ArrayList<>();
    }

    /**
     * 取得指定活動的預設票價資料；分區與票種的實際售價由售票設定服務另外計算。
     */
    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public Map<String, Object> selectOnlyActivitiesPrice(
            BookingSelectOnlyActivitiesPriceRequest request, LoginUser user) {
        final String activity_id = request.activity_id().trim();
        return Boolean.TRUE.equals(user.accessExists())
                ? bookingMapper.selectOnlyActivitiesPrice(activity_id).get(activity_id)
                : new HashMap<>();
    }

    /**
     * 建立一張座位的待付款訂單，保留期限為建立後 10 分鐘。
     * 會員、活動名稱與金額分別取自登入資訊、場次資料及後端計價，不採用前端傳入值。
     * 訂單、保留數、座位與請求去重紀錄在同一交易內寫入；任一步失敗都會回滾。
     *
     * @param request 活動、演出日期時間、座位及可選的票種
     * @param user 目前登入會員，作為訂單歸屬及限購計算依據
     * @param idempotencyKey 同一訂位內容重試時須沿用的識別碼
     * @return HTTP 201 與會員票券資料；已成功處理的相同請求回傳保存的原始回應
     * @throws BookingException 登入失效、識別碼格式或內容衝突、場次不存在、限購或庫存不足
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveTicket(BookingSaveTicketRequest request, LoginUser user, String idempotencyKey) {
        requireAuthenticated(user);
        final String activity_id = request.activity_id().trim();
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new BookingException("INVALID_IDEMPOTENCY_KEY", "請提供有效的 Idempotency-Key", HttpStatus.BAD_REQUEST);
        }
        final String seat = request.seat().trim();
        final String date = request.date().trim();
        final String time = request.time().trim();
        Map<String, Object> sessionMap =
                bookingMapper.getOnlySessionId(activity_id, date, time).get(activity_id);
        if (sessionMap == null) {
            throw new BookingException("SESSION_NOT_FOUND", "找不到活動場次編號", HttpStatus.NOT_FOUND);
        }
        final String session_id = sessionMap.get("session_id").toString();
        final String ticketTypeId = request.ticketTypeId() == null ? "" : request.ticketTypeId().trim();
        final String hash = requestHash(session_id, activity_id, seat, ticketTypeId);
        BookingCoreKey bookingCoreKey = new BookingCoreKey();
        bookingCoreKey.setEmail(user.email());
        bookingCoreKey.setIdempotencyKey(idempotencyKey);
        bookingCoreKey.setHash(hash);
        // 同一會員重送相同識別碼時回傳原結果；內容不同則拒絕，避免網路重試產生重複訂單。
        if (bookingCoreMapper.claimKey(bookingCoreKey) == 0) {
            Map<String, Object> previous = bookingCoreMapper.findKey(bookingCoreKey);
            if (!hash.equals(previous.get("request_hash"))) {
                throw new BookingException("IDEMPOTENCY_CONFLICT", "相同請求識別碼不能用於不同訂位內容", HttpStatus.CONFLICT);
            }
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(previous.get("response_body"));
        }
        // 先鎖定場次再檢查庫存與票價，讓同場次的訂票及設定更新依序執行。
        bookingCoreMapper.lockSession(session_id);
        Map<String, Object> snapshot = bookingCoreMapper.sessionSnapshot(session_id);
        if (snapshot == null || !activity_id.equals(snapshot.get("activity_id"))) {
            throw new BookingException("SESSION_NOT_FOUND", "找不到活動場次", HttpStatus.NOT_FOUND);
        }
        Object activityPrice = snapshot.get("price");
        BigDecimal defaultPrice = activityPrice == null ? null
                : activityPrice instanceof BigDecimal price ? price : new BigDecimal(activityPrice.toString());
        Quote quote = salesSettingsService.quote(session_id, seat, ticketTypeId, user.email(), defaultPrice);
        // 已配置分區的場次在儲存設定時就建立完整座位；未配置的場次才依活動佈局補建。
        if (quote.zoneId() == null) {
            bookingCoreMapper.ensureSeat(session_id, seat);
        }
        List<Map<String, Object>> data;
        final String accessJwt = user.email();
        BookingSession bookingSession = new BookingSession();
        bookingSession.setSession_id(session_id);
        int cnt = bookingMapper.updateSession(bookingSession);
        String createdOrderNo;
        if (cnt == 1) {
            BookingSaveTicket bookingSaveTicket = new BookingSaveTicket();
            // 訂單編號由 SQL 以 CT＋當天日期＋每日流水號產生；流水號目前不補零。
            bookingSaveTicket.setSession_id(session_id);
            bookingSaveTicket.setEmail(accessJwt);
            bookingSaveTicket.setName(snapshot.get("name").toString());
            bookingSaveTicket.setDate(snapshot.get("date").toString());
            bookingSaveTicket.setTime(snapshot.get("time").toString());
            BigDecimal price = quote.price();
            bookingSaveTicket.setZoneId(quote.zoneId());
            bookingSaveTicket.setTicketTypeId(quote.ticketTypeId());
            bookingSaveTicket.setZoneName(quote.zoneName());
            bookingSaveTicket.setTicketTypeName(quote.ticketTypeName());
            bookingSaveTicket.setStatus(bookingOrderStatus.PENDING_PAYMENT.name());
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
            // 必須成功保留這張座位；失敗會拋出例外，連同先前的庫存與訂單寫入一起回滾。
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

            // 等資料庫提交成功才通知與排程，避免使用者收到已回滾訂單的付款訊息。
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        /** 已提交的新訂單開始安排付款提醒與逾期處理，並發送建立成功通知。 */
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
            BookingCompleteKey bookingCompleteKey = new BookingCompleteKey();
            bookingCompleteKey.setEmail(user.email());
            bookingCompleteKey.setIdempotencyKey(idempotencyKey);
            bookingCompleteKey.setCreatedOrderNo(createdOrderNo);
            bookingCompleteKey.setBody(body);
            BookingException.requireOne(
                    bookingCoreMapper.completeKey(bookingCompleteKey)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot store booking result", ex);
        }
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 以 JVM 預設時區輸出 yyyy-MM-dd HH:mm:ss，供付款期限通知顯示。
     */
    private String dateFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * 將場次、活動、座位及非空票種序列化成 JSON，再計算 SHA-256；未指定票種時沿用三欄位格式。
     */
    private String requestHash(String session, String activity, String seat, String ticketTypeId) {
        try {
            byte[] content = new ObjectMapper().writeValueAsBytes(
                    ticketTypeId.isEmpty() ? List.of(session, activity, seat) : List.of(session, activity, seat, ticketTypeId)
            );
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to generate request hash", ex);
        }
    }

    /**
     * 要求 Redis 仍有此 token 的登入紀錄且未被列入黑名單；不符合時拋出 HTTP 401 訂票例外。
     */
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

    /**
     * 供交易提交後的排程與通知使用；資料回滾時不執行這些動作。
     */
    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /** 交易提交成功時執行呼叫端提供的動作。 */
            @Override
            public void afterCommit() { action.run(); }
        });
    }

    /**
     * 要求訂單屬於登入會員與指定場次，將有效待付款訂單取消並釋放保留；提交成功後撤銷排程及發送通知。
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> cancelOrder(BookingCanceTicketRequest request, LoginUser user) {
        requireAuthenticated(user);
        String orderno = request.orderno().trim();
        BookingOrder bookingOrder = new BookingOrder();
        bookingOrder.setOrderno(orderno);
        bookingOrder.setSession_id(request.session_id());
        bookingOrder.setEmail(user.email());
        bookingOrder.setStatus(bookingOrderStatus.CANCELLED);
        bookingOrderService.transition(bookingOrder);
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

    /**
     * 僅在登入紀錄存在、token 未列入黑名單且請求狀態為待付款時，依場次條件查詢開賣日期與時間。
     */
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
                if (bookingOrderStatus.PENDING_PAYMENT.name().equals(ticket_status)) {
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

    /**
     * 將有效待付款訂單轉為 PAID，把保留座位轉成售出，提交後撤銷排程及通知；此流程未呼叫外部金流。
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> dopayprice(BookingDopaypriceRequest request, LoginUser user) {
        requireAuthenticated(user);
        String orderno = request.orderno().trim();
        BookingOrder bookingOrder = new BookingOrder();
        bookingOrder.setOrderno(orderno);
        bookingOrder.setSession_id(request.session_id());
        bookingOrder.setEmail(user.email());
        bookingOrder.setStatus(bookingOrderStatus.PAID);
        bookingOrderService.transition(bookingOrder);
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

    /**
     * 依活動的排號清單與每排座位數，產生含 id、row、number 的座位列表；未登入或缺少配置時回傳空清單。
     */
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
                for (String rawRow : rows) {
                    String row = rawRow.trim();
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

    /**
     * 先由活動、日期及時間找出場次，再讀取該場次非 AVAILABLE 的座位；找不到場次時回傳空清單。
     */
    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<String> selectOnlyUnavailableSeats(
            BookingSelectOnlyUnavailableSeatsRequest request, LoginUser user) {
        requireAuthenticated(user);
        final String activityId = request.activity_id().trim();
        final String selectedDate = request.selected_date().trim();
        final String selectedSession = request.selected_session().trim();
        Map<String, Object> sessionMap =
                bookingMapper.getOnlySessionId(activityId, selectedDate, selectedSession).get(activityId);
        if (sessionMap == null) {
            return List.of();
        }
        final String sessionId = sessionMap.get("session_id").toString();
        List<String> seatsList = bookingCoreMapper.unavailableSeats(sessionId);
        return seatsList.isEmpty()
                ? List.of()
                : seatsList;
    }

}
