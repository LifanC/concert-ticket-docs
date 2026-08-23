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

import java.math.BigDecimal;
import java.util.*;

@Service
public class BookingServiceImpl implements BookingService {

    private final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingMapper bookingMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenService jwtTokenService;
    private final NotifierConsumer notifier;

    public BookingServiceImpl(
            BookingMapper bookingMapper,
            StringRedisTemplate stringRedisTemplate,
            JwtTokenService jwtTokenService,
            NotifierConsumer notifier
    ) {
        this.bookingMapper = bookingMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtTokenService = jwtTokenService;
        this.notifier = notifier;
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
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveTicket(BookingSaveTicketRequest request) {
        final String orderno = request.getOrderno().trim();
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
                    BookingSaveTicket bookingSaveTicket = new BookingSaveTicket();
                    bookingSaveTicket.setOrderno(orderno);
                    bookingSaveTicket.setSession_id(session_id);
                    bookingSaveTicket.setCustomer(emailCutOff);
                    bookingSaveTicket.setEmail(accessJwt);
                    bookingSaveTicket.setName(name);
                    bookingSaveTicket.setDate(date);
                    bookingSaveTicket.setTime(time);
                    bookingSaveTicket.setStatus(ticket_status);
                    BigDecimal price = bookingMapper.selectActivityPrice(activity_id);
                    if (price == null) {
                        throw new IllegalArgumentException("活動不存在");
                    }
                    bookingSaveTicket.setPrice(price);
                    int inserted = bookingMapper.saveTicket(bookingSaveTicket);
                    if (inserted == 0) {
                        throw new IllegalStateException("訂單編號已存在");
                    }
                    data = new ArrayList<>(bookingMapper.selectOnlyTicket(emailCutOff));

                    NotificationMessage message =
                            new NotificationMessage(
                                    accessJwt,
                                    "新通知",
                                    "尚未付款"
                            );
                    notifier.sendNotification(message);
                }
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("{} : (新增訂單)無效的 JWT token", orderno);
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

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> cancelOrder(BookingCanceTicketRequest request) {
        final String orderno = request.getOrderno().trim();
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
                    BookingSaveTicket bookingSaveTicket = new BookingSaveTicket();
                    bookingSaveTicket.setOrderno(orderno);
                    bookingSaveTicket.setCustomer(accessJwt.substring(0, accessJwt.indexOf('@')));
                    bookingSaveTicket.setStatus("已取消");
                    int cnt = bookingMapper.cancelTicket(bookingSaveTicket);
                    if (cnt > 0) {
                        dataMap.put("judge", true);
                    }
                    data.add(dataMap);
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
        final String name = request.getName().trim();
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
                    SalesDate salesDate = new SalesDate();
                    salesDate.setActivity(name);
                    salesDate.setDate(date);
                    salesDate.setTime(time);
                    List<Map<String, Object>> data = bookingMapper.sessionSalesDate(salesDate);
                    if (!data.isEmpty()) {
                        dataMap = data.getFirst();
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
        final String activity = request.getActivity().trim();
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
                BookingDopaypriceTicket bookingDopaypriceTicket = new BookingDopaypriceTicket();
                bookingDopaypriceTicket.setOrderno(orderno);
                bookingDopaypriceTicket.setCustomer(accessJwt.substring(0, accessJwt.indexOf('@')));
                bookingDopaypriceTicket.setActivity(activity);
                bookingDopaypriceTicket.setDate(date);
                bookingDopaypriceTicket.setTime(time);
                int cnt = bookingMapper.dopaypriceTicket(bookingDopaypriceTicket);
                if (cnt > 0) {
                    dataMap.put("judge", true);

                    NotificationMessage message =
                            new NotificationMessage(
                                    accessJwt,
                                    "新通知",
                                    "付款成功"
                            );
                    notifier.sendNotification(message);
                }
                data.add(dataMap);
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
