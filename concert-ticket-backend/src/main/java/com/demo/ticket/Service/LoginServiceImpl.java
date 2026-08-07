package com.demo.ticket.Service;

import com.demo.ticket.Common.ConvertFormat;
import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Dto.Login.*;
import com.demo.ticket.Exception.*;
import com.demo.ticket.Mapper.LoginMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    private final Logger logger = LoggerFactory.getLogger(LoginServiceImpl.class);

    // ? redis 到期時間 Seconds
    @Value("${jwt.refreshExpirationSeconds}")
    private long refreshExpirationSeconds;

    @Value("${jwt.accessExpirationSeconds}")
    private long accessExpirationSeconds;

    /*
     * 防 Cache Stampede（雪崩）
     * 問題：* 大量 key 同時過期 → DB 被打爆
     * */
    private int refreshExpirationSecondsAddRndomNumber() {
        int min = 1;
        int max = 60;
        return Math.toIntExact(refreshExpirationSeconds + (new Random().nextInt((max - min) + 1) + min));
    }
    private int accessExpirationSecondsAddRndomNumber() {
        int min = 1;
        int max = 60;
        return Math.toIntExact(accessExpirationSeconds + (new Random().nextInt((max - min) + 1) + min));
    }

    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginMapper loginMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtTokenService jwtTokenService;

    public LoginServiceImpl(
            BCryptPasswordEncoder passwordEncoder,
            LoginMapper loginMapper,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            JwtTokenService jwtTokenService
    ) {
        this.passwordEncoder = passwordEncoder;
        this.loginMapper = loginMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public ResponseEntity<?> register(RegisterRequest request) {
        final String name = request.getName().trim();
        final String email = request.getEmail().trim();
        final String phone = request.getPhone().trim();
        final String password = request.getPassword().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        Register register = new Register(name, email, phone, passwordEncoder.encode(password));
        Map<String, Object> dataMap = new TreeMap<>();
        try {
            loginMapper.create(register);
        } catch (DuplicateKeyException e) {
            logger.error("電子信箱或帳號已存在", e);
        }
        dataMap.put("remark", "註冊成功");
        dataMap.put("name", name);
        dataMap.put("email", email);
        dataMap.put("phone", phone);
        dataMap.put("created_date", ConvertFormat.time(""));
        dataMap.put("updated_date", "");
        data.add(dataMap);
        HttpStatus status = HttpStatus.CREATED;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    public ResponseEntity<?> login(LoginRequest request) {
        final String account = request.getAccount().trim();
        final String password = request.getPassword().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        int idx = account.indexOf('@');
        String accountCutOff = idx >= 0 ? account.substring(0, idx) : account;
        Map<String, Object> userDataSelect;
        final String userDataOnly = String.format(
                RedisKey.redisUserDataKey.get("userDataOnly"),
                accountCutOff
        );
        Login login = new Login(accountCutOff);
        String json = stringRedisTemplate.opsForValue().get(userDataOnly);
        if (json != null) {
            userDataSelect = objectMapper.readValue(json, new TypeReference<>() {});
        } else {
            userDataSelect = loginMapper.select(login).get(login.getAccount());
            String jsonMap = objectMapper.writeValueAsString(userDataSelect);
            stringRedisTemplate.opsForValue().set(
                    userDataOnly, jsonMap, Duration.ofSeconds(refreshExpirationSecondsAddRndomNumber()));
        }
        Map<String, Object> dataMap = new TreeMap<>();
        String remark = "登入失敗";
        boolean judge = false;
        if (userDataSelect != null) {
            final String userDataPassword = userDataSelect.get("password").toString();
            if (passwordEncoder.matches(password, userDataPassword)) {
                final String jti = UUID.randomUUID().toString();
                int refreshExpirationSecondsAddRndomNumber = refreshExpirationSecondsAddRndomNumber();
                Boolean success = jwtTokenService.createRefreshToken(
                        jti,
                        refreshExpirationSecondsAddRndomNumber,
                        login.getAccount()
                );
                if (Boolean.FALSE.equals(success)) {
                    logger.error("{} : (登入 Token)已經存在", account);
                }
                logger.error("{} : (登入 Token)成功", account);
                remark = "登入成功";
                judge = true;
            }
        }
        dataMap.put("remark", remark);
        dataMap.put("account", login.getAccount());
        dataMap.put("judge", judge);
        data.add(dataMap);
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    private void redisDels(String accessRedisKey, int cnt) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(accessRedisKey)
                .count(cnt)
                .build();
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                List<String> keysToDelete = new ArrayList<>();
                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    keysToDelete.add(key);
                    // 批量刪除：每 ? 個 key 刪一次，避免一次性刪太多
                    if (keysToDelete.size() >= cnt) {
                        stringRedisTemplate.delete(keysToDelete);
                        keysToDelete.clear();
                    }
                }
                // 刪除剩下的
                if (!keysToDelete.isEmpty()) {
                    stringRedisTemplate.delete(keysToDelete);
                }
            }
            return null;
        });
    }

    @Override
    public ResponseEntity<?> validate(LoginTokenValidateRequest request) {
        final String account = request.getAccount().trim();
        int idx = account.indexOf('@');
        String accountCutOff = idx >= 0 ? account.substring(0, idx) : account;
        List<Map<String, Object>> data = new ArrayList<>();
        final String refreshRedisKey = String.format(
                RedisKey.redisKey.get("refresh"),
                accountCutOff
        );
        Boolean exists = stringRedisTemplate.hasKey(refreshRedisKey);
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("remark", "驗證失敗");
        dataMap.put("account", accountCutOff);
        dataMap.put("accessToken", "");
        dataMap.put("name", "");
        dataMap.put("email", "");
        dataMap.put("phone", "");
        dataMap.put("judge", false);
        if (Boolean.TRUE.equals(exists)) {
            try {
                Claims claims = jwtTokenService.refreshTokenInRedis(refreshRedisKey);
                String accountJwt = claims.getSubject();
                final String accessJtId = UUID.randomUUID().toString();
                int accessExpirationSecondsAddRndomNumber = accessExpirationSecondsAddRndomNumber();
                String accessToken = jwtTokenService.createAccessToken(
                        accessExpirationSecondsAddRndomNumber,
                        accessJtId,
                        accountJwt
                );
                String accessRedisKey = String.format(
                        RedisKey.redisKey.get("access"),
                        "*",
                        accountJwt
                );
                // 避免 Redis key 無限制增加導致記憶體耗盡
                int cnt = 5;
                ScanOptions options = ScanOptions.scanOptions()
                        .match(accessRedisKey)
                        .count(cnt)
                        .build();
                // redis(指定key)的數量
                Long redisCount =
                        stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
                            long count = 0;
                            try (Cursor<byte[]> cursor = connection.scan(options)) {
                                while (cursor.hasNext()) {
                                    cursor.next();
                                    count++;
                                }
                            }
                            return count;
                        });
                redisCount = redisCount == null ? 0L : redisCount;
                // redis(指定key)的上限數量
                int maximumQuantity = 20;
                if (redisCount >= maximumQuantity) {
                    // Redis「我希望每次 SCAN 返回大約 5 個 key」
                    // 這是一個 建議值，Redis 可能返回多於或少於這個數量，取決於內部算法。
                    redisDels(accessRedisKey, cnt);
                }
                accessRedisKey = String.format(
                        RedisKey.redisKey.get("access"),
                        accessJtId,
                        accountJwt
                );
                Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                        accessRedisKey,
                        accessToken,
                        Duration.ofSeconds(accessExpirationSecondsAddRndomNumber())
                );
                if (Boolean.FALSE.equals(success)) {
                    logger.error("{} : (驗證)Token 已經存在", account);
                }
                logger.error("{} : (驗證)Token 成功", account);

                final String blacklistRedisKey = String.format(
                        RedisKey.redisKey.get("blacklist"),
                        accessJtId
                );
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistRedisKey))) {
                    logger.error("{} : (驗證)Token 已被撤銷", accountJwt);
                } else {
                    Map<String, Object> userDataSelect = null;
                    final String userDataOnly = String.format(
                            RedisKey.redisUserDataKey.get("userDataOnly"),
                            accountCutOff
                    );
                    String json = stringRedisTemplate.opsForValue().get(userDataOnly);
                    if (json != null) {
                        userDataSelect = objectMapper.readValue(json, new TypeReference<>() {});
                    }
                    if (userDataSelect != null) {
                        dataMap.put("remark", "驗證成功");
                        dataMap.put("accessToken", accessToken);
                        dataMap.put("name", ObjectUtils.toString(userDataSelect.get("name")));
                        dataMap.put("email", ObjectUtils.toString(userDataSelect.get("email")));
                        dataMap.put("phone", ObjectUtils.toString(userDataSelect.get("phone")));
                        dataMap.put("birthday", ObjectUtils.toString(userDataSelect.get("birthday")));
                        dataMap.put("judge", true);
                    }
                }
            } catch (JwtException e) {
                // JWT 不合法
                logger.error("{} : (Token驗證)無效的 JWT token", accountCutOff);
                throw new JwtException("無效的 JWT token", e);
            }
        }
        data.add(dataMap);
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    public ResponseEntity<?> saveProfile(LoginSaveProfileRequest request) {
        final String name = request.getName().trim();
        final String email = request.getEmail().trim();
        final String phone = request.getPhone().trim();
        final String birthday = request.getBirthday().trim();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        String emailCutOff = email.substring(0, email.indexOf('@'));
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("remark", "修改會員資料失敗");
        dataMap.put("name", name);
        dataMap.put("email", email);
        dataMap.put("phone", "");
        dataMap.put("birthday", "");
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
            logger.error("{}(權限{}) : (修改會員資料)有效的 JWT UUID {}", accessJwt, accessAuthorities, accessJtId);
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (修改會員資料) Token 已過期", accessJwt);
            } else {
                final String blacklistRedisKey = String.format(
                        RedisKey.redisKey.get("blacklist"),
                        accessJtId
                );
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistRedisKey))) {
                    logger.error("{} : (修改會員資料)Token 已被撤銷", accessJwt);
                } else {
                    LoginSaveProfile loginSaveProfile = new LoginSaveProfile();
                    loginSaveProfile.setName(name);
                    loginSaveProfile.setEmail(email);
                    loginSaveProfile.setPhone(phone);
                    loginSaveProfile.setBirthday(birthday);
                    loginMapper.save(loginSaveProfile);
                    dataMap.put("remark", "修改會員資料成功");
                    dataMap.put("phone", phone);
                    dataMap.put("birthday", birthday);
                    dataMap.put("judge", true);
                }
            }
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("{} : (修改會員資料)無效的 JWT token", emailCutOff);
            throw new JwtException("無效的 JWT token", e);
        }
        data.add(dataMap);
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    public ResponseEntity<?> logout(LoginLogoutRequest request) {
        final String email = request.getEmail().trim();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        String emailCutOff = email.substring(0, email.indexOf('@'));
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("remark", "登出失敗");
        dataMap.put("email", email);
        dataMap.put("judge", false);
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJtId = accessClaims.getId();
            String accessJwt = accessClaims.getSubject();
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            long remainingSeconds = stringRedisTemplate.getExpire(accessRedisKey, TimeUnit.SECONDS);
            if (remainingSeconds > 0) {
                final String blacklistRedisKey = String.format(
                        RedisKey.redisKey.get("blacklist"),
                        accessJtId
                );
                stringRedisTemplate.opsForValue().set(
                        blacklistRedisKey,
                        "1",
                        Duration.ofSeconds(remainingSeconds)
                );
            }
            stringRedisTemplate.delete(accessRedisKey);
            final String refreshRedisKey = String.format(
                    RedisKey.redisKey.get("refresh"),
                    emailCutOff
            );
            stringRedisTemplate.delete(refreshRedisKey);

            final String userDataOnly = String.format(
                    RedisKey.redisUserDataKey.get("userDataOnly"),
                    emailCutOff
            );
            stringRedisTemplate.delete(userDataOnly);
            dataMap.put("remark", "登出成功");
            dataMap.put("judge", true);
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("{} : (登出)無效的 JWT token", emailCutOff);
            throw new JwtException("無效的 JWT token", e);
        }
        data.add(dataMap);
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

}
