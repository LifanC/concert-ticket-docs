package com.demo.ticket.Service;

import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Config.WebSocket.NotificationMessage;
import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Dto.Booking.BookingCanceTicketRequest;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.BookingSaveTicketRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class BookingServiceImpl implements  BookingService {

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
    public List<Map<String, Object>> selectOnlyActivities(String activityName) {
        return bookingMapper.selectOnlyActivities(activityName);
    }

    @Override
    public List<Map<String, Object>> selectOnlySession(String date) {
        return bookingMapper.selectOnlySession(date);
    }

    @Override
    public List<Map<String, Object>> selectOnlyTicket(String email) {
        return bookingMapper.selectOnlyTicket(email.substring(0, email.indexOf('@')));
    }

    @Override
    public Map<String, Object> selectOnlyActivitiesPrice(String activityId) {
        return bookingMapper.selectOnlyActivitiesPrice(activityId).get(activityId);
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveTicket(BookingSaveTicketRequest request) {
        final String orderno = request.getOrderno().trim();
        final String email = request.getEmail().trim();
        final String name = request.getName().trim();
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        final String bookingStatus = request.getStatus().trim();
        final BigDecimal price = request.getPrice();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        String emailCutOff = email.substring(0, email.indexOf('@'));
        final String refreshRedisKey = String.format(
                RedisKey.redisKey.get("refresh"),
                emailCutOff
        );
        try {
            jwtTokenService.refreshTokenInRedis(refreshRedisKey);
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
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
                    bookingSaveTicket.setCustomer(emailCutOff);
                    bookingSaveTicket.setEmail(email);
                    bookingSaveTicket.setName(name);
                    bookingSaveTicket.setDate(date);
                    bookingSaveTicket.setTime(time);
                    bookingSaveTicket.setStatus(bookingStatus);
                    bookingSaveTicket.setPrice(price);
                    bookingMapper.saveTicket(bookingSaveTicket);
                    data = new ArrayList<>(bookingMapper.selectOnlyTicket(emailCutOff));

                    NotificationMessage message =
                            new NotificationMessage(
                                    email,
                                    "新通知",
                                    "建立新的訂單"
                            );
                    notifier.sendNotification(message);
                }
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("{} : (新增訂單)無效的 JWT token", emailCutOff);
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
        final String email = request.getEmail().trim();
        final String bookingStatus = request.getStatus().trim();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        String emailCutOff = email.substring(0, email.indexOf('@'));
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("judge", false);
        final String refreshRedisKey = String.format(
                RedisKey.redisKey.get("refresh"),
                emailCutOff
        );
        try {
            jwtTokenService.refreshTokenInRedis(refreshRedisKey);
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
                    bookingSaveTicket.setStatus(bookingStatus);
                    int cnt = bookingMapper.cancelTicket(bookingSaveTicket);
                    if (cnt > 0) {
                        dataMap.put("judge", true);
                    }
                    data.add(dataMap);
                }
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("{} : (取消訂單)無效的 JWT token", emailCutOff);
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
