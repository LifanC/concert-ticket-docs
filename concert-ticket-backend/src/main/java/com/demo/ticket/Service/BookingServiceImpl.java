package com.demo.ticket.Service;

import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Config.WebSocket.NotificationMessage;
import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.Mapper.BookingMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingMapper bookingMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenService jwtTokenService;
    private final NotifierConsumer notifier;
    private final BookingPaymentScheduler bookingPaymentScheduler;

    public BookingServiceImpl(
            BookingMapper bookingMapper,
            StringRedisTemplate stringRedisTemplate,
            JwtTokenService jwtTokenService,
            NotifierConsumer notifier,
            BookingPaymentScheduler bookingPaymentScheduler
    ) {
        this.bookingMapper = bookingMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtTokenService = jwtTokenService;
        this.notifier = notifier;
        this.bookingPaymentScheduler = bookingPaymentScheduler;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyActivities(BookingSelectOnlyActivitiesRequest request) {
        final String accessToken = request.getToken().trim();
        final String activity_id = request.getActivity_id().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> accessAuthorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (單一活動)有效的 JWT UUID {}", accessJwt, accessAuthorities, accessJtId);
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (單一活動) Token 已過期", accessJwt);
            } else {
                data = bookingMapper.selectOnlyActivities(activity_id);
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(單一活動)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        return data;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlySession(BookingSelectOnlySessionRequest request) {
        final String accessToken = request.getToken().trim();
        final String date = request.getDate().trim();
        final String activity_id = request.getActivity_id().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> accessAuthorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (單一場次資料)有效的 JWT UUID {}", accessJwt, accessAuthorities, accessJtId);
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (單一場次資料) Token 已過期", accessJwt);
            } else {
                data = bookingMapper.selectOnlySession(date, activity_id);
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(單一場次資料)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        return data;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyTicket(BookingSelectOnlyTicketRequest request) {
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> accessAuthorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (訂單資料)有效的 JWT UUID {}", accessJwt, accessAuthorities, accessJtId);
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (訂單資料) Token 已過期", accessJwt);
            } else {
                data = bookingMapper.selectOnlyTicket(accessJwt.substring(0, accessJwt.indexOf('@')));
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(訂單資料)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        return data;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public Map<String, Object> selectOnlyActivitiesPrice(BookingSelectOnlyActivitiesPriceRequest request) {
        final String activity_id = request.getActivity_id().trim();
        final String accessToken = request.getToken().trim();
        Map<String, Object> data = new HashMap<>();
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> accessAuthorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (單一場次金額)有效的 JWT UUID {}", accessJwt, accessAuthorities, accessJtId);
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (單一場次金額) Token 已過期", accessJwt);
            } else {
                data = bookingMapper.selectOnlyActivitiesPrice(activity_id).get(activity_id);
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(單一場次金額)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        return data;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveTicket(BookingSaveTicketRequest request) {
        final String session_id = request.getSession_id().trim();
        final String activity_id = request.getActivity_id().trim();
        final String name = request.getName().trim();
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        final String ticket_status = request.getStatus().trim();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            final String emailCutOff = accessJwt.substring(0, accessJwt.indexOf('@'));
            List<String> accessAuthorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (新增訂單)有效的 JWT UUID {}", accessJwt, accessAuthorities, accessJtId);
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (新增訂單) Token 已過期", accessJwt);
            } else {
                final String blacklistRedisKey = String.format(
                        RedisKey.redisKey.get("blacklist"),
                        accessJtId
                );
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistRedisKey))) {
                    logger.error("{} : (新增訂單)Token 已被撤銷", accessJwt);
                } else {
                    BookingSession bookingSession = new BookingSession();
                    bookingSession.setSession_id(session_id);
                    int cnt = bookingMapper.updateSession(bookingSession);
                    if (cnt > 0) {
                        BookingSaveTicket bookingSaveTicket = new BookingSaveTicket();
                        // "訂單編號格式需為 CTYYYYMMDDNNN，例如 CT20260815001" SQL處理
                        bookingSaveTicket.setSession_id(session_id);
                        bookingSaveTicket.setCustomer(emailCutOff);
                        bookingSaveTicket.setEmail(accessJwt);
                        bookingSaveTicket.setName(name);
                        bookingSaveTicket.setDate(date);
                        bookingSaveTicket.setTime(time);
                        BigDecimal price = bookingMapper.selectActivityPrice(activity_id);
                        bookingSaveTicket.setStatus(ticket_status);
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
                        logger.info("新增訂單成功，訂單編號：{}", orderno);
                        bookingSaveTicket.setOrderno(orderno);
                        data = new ArrayList<>(bookingMapper.selectOnlyTicket(emailCutOff));

                        // Transaction commit 成功後，安排 expires_at 時執行
                        TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {
                                    @Override
                                    public void afterCommit() {
                                        bookingPaymentScheduler.scheduleExpiration(accessJwt, bookingSaveTicket);
                                    }
                                }
                        );

                        Map<String, Object> sessionData = bookingMapper.selectOnlySessionId(session_id).get(session_id);
                        BigDecimal available = new BigDecimal(sessionData.get("available").toString());
                        NotificationMessage message =
                                new NotificationMessage(
                                        accessJwt,
                                        "新通知：請在 " + minutes + " 分鐘內完成付款，剩餘庫存：" + available,
                                        "尚未付款，付款期限：" +
                                                dateFormat(dateNow) + " ～ " + dateFormat(dateExpiresAt)
                                );
                        notifier.sendNotification(message);
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
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(新增訂單)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
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
    public ResponseEntity<?> cancelOrder(BookingCanceTicketRequest request) {
        final String orderno = request.getOrderno().trim();
        final String session_id = request.getSession_id().trim();
        final String ticket_status = request.getStatus().trim();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("judge", false);
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> accessAuthorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (取消訂單)有效的 JWT UUID {}", accessJwt, accessAuthorities, accessJtId);
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (取消訂單) Token 已過期", accessJwt);
            } else {
                final String blacklistRedisKey = String.format(
                        RedisKey.redisKey.get("blacklist"),
                        accessJtId
                );
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistRedisKey))) {
                    logger.error("{} : (取消訂單)Token 已被撤銷", accessJwt);
                } else {
                    BookingSession bookingSession = new BookingSession();
                    bookingSession.setSession_id(session_id);
                    int cntUpdateSession = bookingMapper.cancelSession(bookingSession);
                    if (cntUpdateSession > 0) {
                        BookingSaveTicket bookingSaveTicket = new BookingSaveTicket();
                        bookingSaveTicket.setOrderno(orderno);
                        bookingSaveTicket.setCustomer(accessJwt.substring(0, accessJwt.indexOf('@')));
                        if ("PENDING_PAYMENT".equals(ticket_status)) {
                            bookingSaveTicket.setStatus("CANCELLED");
                            Date dateNow = new Date();
                            bookingSaveTicket.setCancelled_at(dateNow);
                            int cntCancelTicket = bookingMapper.cancelTicket(bookingSaveTicket);
                            if (cntCancelTicket > 0) {
                                dataMap.put("judge", true);
                            }
                        }

                        TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {
                                    @Override
                                    public void afterCommit() {
                                        boolean cancelled = bookingPaymentScheduler.cancelExpiration(orderno);
                                        logger.info(
                                                "訂單 {} 排程取消結果：{}",
                                                orderno,
                                                cancelled
                                        );
                                    }
                                }
                        );

                        data.add(dataMap);
                    }
                }
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(取消訂單)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
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
    public Map<String, Object> sessionSalesDate(BookingSessionSalesDateRequest request) {
        final String session_id = request.getSession_id().trim();
        final String activity_id = request.getActivity_id().trim();
        final String ticket_status = request.getStatus().trim();
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        final String accessToken = request.getToken().trim();
        Map<String, Object> dataMap = new HashMap<>();
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> accessAuthorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (售賣日期)有效的 JWT UUID {}", accessJwt, accessAuthorities, accessJtId);
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (售賣日期) Token 已過期", accessJwt);
            } else {
                final String blacklistRedisKey = String.format(
                        RedisKey.redisKey.get("blacklist"),
                        accessJtId
                );
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistRedisKey))) {
                    logger.error("{} : (售賣日期)Token 已被撤銷", accessJwt);
                } else {
                    if ("PENDING_PAYMENT".equals(ticket_status)) {
                        BookingSalesDate bookingSalesDate = new BookingSalesDate();
                        bookingSalesDate.setSession_id(session_id);
                        bookingSalesDate.setActivity_id(activity_id);
                        bookingSalesDate.setDate(date);
                        bookingSalesDate.setTime(time);
                        dataMap = bookingMapper.sessionSalesDate(bookingSalesDate).get(session_id);
                        ;
                    }
                }
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(售賣日期)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        return dataMap;

    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> dopayprice(BookingDopaypriceRequest request) {
        final String orderno = request.getOrderno().trim();
        final String session_id = request.getSession_id().trim();
        final String ticket_status = request.getStatus().trim();
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("judge", false);
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> accessAuthorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (付款)有效的 JWT UUID {}", accessJwt, accessAuthorities, accessJtId);
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (付款) Token 已過期", accessJwt);
            } else {
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
                    if ("PENDING_PAYMENT".equals(ticket_status)) {
                        bookingDopaypriceTicket.setStatus("PAID");
                        Date dateNow = new Date();
                        bookingDopaypriceTicket.setPaid_at(dateNow);
                        int cntCancelTicket = bookingMapper.dopaypriceTicket(bookingDopaypriceTicket);
                        if (cntCancelTicket > 0) {
                            dataMap.put("judge", true);

                            NotificationMessage message =
                                    new NotificationMessage(
                                            accessJwt,
                                            "付款成功",
                                            "您的票券已付款成功，可至訂單頁面查看票券資訊"
                                    );
                            notifier.sendNotification(message);
                        }
                    }
                    data.add(dataMap);
                }
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(付款)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }
}
