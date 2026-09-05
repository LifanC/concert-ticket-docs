package com.demo.ticket.Service;

import com.demo.ticket.Common.ConvertFormat;
import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Dto.Login.*;
import com.demo.ticket.Exception.*;
import com.demo.ticket.Mapper.LoginMapper;
import com.demo.ticket.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class LoginServiceImpl implements LoginService {

    @Value("${auth.cookie.secure:false}")
    private boolean refreshCookieSecure;

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
        final String account = request.getAccount().trim();
        final String name = request.getName().trim();
        final String email = request.getEmail().trim();
        final String phone = request.getPhone().trim();
        final String password = request.getPassword().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        Register register = new Register(account, name, email, phone, passwordEncoder.encode(password));
        Map<String, Object> dataMap = new TreeMap<>();
        loginMapper.create(register);
        dataMap.put("remark", "註冊成功");
        dataMap.put("account", account);
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
        String remark = "登入失敗";
        boolean judge = false;
        String refreshToken = null;
        int refreshTokenMaxAge = 0;
        Login login = new Login(account);
        Map<String, Object> dataMap = new TreeMap<>();
        Map<String, Object> userDataSelect = loginMapper.select(login);
        if (userDataSelect != null && !userDataSelect.isEmpty()) {
            final String email = userDataSelect.get("email").toString();
            final String userDataOnly = String.format(
                    RedisKey.redisUserDataKey.get("userDataOnly"),
                    email
            );
            String jsonMap = objectMapper.writeValueAsString(userDataSelect);
            stringRedisTemplate.opsForValue().set(
                    userDataOnly, jsonMap, Duration.ofSeconds(refreshExpirationSecondsAddRndomNumber()));
            final String userDataPassword = userDataSelect.get("password").toString();
            if (passwordEncoder.matches(password, userDataPassword)) {
                String jti = UUID.randomUUID().toString();
                int refreshExpirationSecondsAddRndomNumber = refreshExpirationSecondsAddRndomNumber();
                // 建立 refresh token
                final String refreshRedisKey = String.format(
                        RedisKey.redisKey.get("refresh"),
                        jti,
                        email
                );
                refreshToken = jwtTokenService.createRefreshToken(
                        refreshRedisKey,
                        jti,
                        refreshExpirationSecondsAddRndomNumber,
                        email
                );
                stringRedisTemplate.opsForValue().set(
                        refreshRedisKey,
                        refreshToken,
                        Duration.ofSeconds(refreshExpirationSecondsAddRndomNumber)
                );
                refreshTokenMaxAge = refreshExpirationSecondsAddRndomNumber;
                remark = "登入成功";
                judge = true;

                final String refreshJtiRedisKey = String.format(
                        RedisKey.redisKey.get("refreshJti"),
                        email
                );
                String currentJti = stringRedisTemplate.opsForValue().get(refreshJtiRedisKey);
                Boolean accessExists = stringRedisTemplate.hasKey(refreshJtiRedisKey);
                if (Boolean.TRUE.equals(accessExists)) {
                    final String refreshRedisKeyOld = String.format(
                            RedisKey.redisKey.get("refresh"),
                            currentJti,
                            email
                    );
                    final String refreshJtiRedisKeyOld = String.format(
                            RedisKey.redisKey.get("refreshJti"),
                            email
                    );
                    stringRedisTemplate.delete(refreshJtiRedisKeyOld);
                    stringRedisTemplate.delete(refreshRedisKeyOld);
                } else {
                    stringRedisTemplate.opsForValue().set(
                            refreshJtiRedisKey,
                            jti,
                            Duration.ofSeconds(refreshExpirationSecondsAddRndomNumber)
                    );
                }
            }
        }
        dataMap.put("remark", remark);
        dataMap.put("account", login.getAccount());
        dataMap.put("judge", judge);
        data.add(dataMap);
        HttpStatus status = HttpStatus.OK;
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (refreshToken != null) {
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(refreshCookieSecure)
                    .sameSite("Lax")
                    .path("/api/v1/login")
                    .maxAge(refreshTokenMaxAge)
                    .build();
            response.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return response
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
    public ResponseEntity<?> validate(String refreshToken) {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("remark", "驗證失敗");
        dataMap.put("accessToken", "");
        dataMap.put("name", "");
        dataMap.put("email", "");
        dataMap.put("phone", "");
        dataMap.put("judge", false);
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Claims claims = jwtTokenService.validateRefreshToken(refreshToken);
                final String accessJtId = claims.getId();
                final String accountJwt = claims.getSubject();
                final String userDataOnly = String.format(
                        RedisKey.redisUserDataKey.get("userDataOnly"),
                        accountJwt
                );
                String json = stringRedisTemplate.opsForValue().get(userDataOnly);
                if (json != null) {
                    Map<String, Object> userDataSelect = objectMapper.readValue(json, new TypeReference<>() {});
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
                    int cnt = 100;
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
                    stringRedisTemplate.opsForValue().setIfAbsent(
                            accessRedisKey,
                            accessToken,
                            Duration.ofSeconds(accessExpirationSecondsAddRndomNumber())
                    );
                    dataMap.put("remark", "驗證成功");
                    dataMap.put("accessToken", accessToken);
                    dataMap.put("name", userDataSelect.get("name").toString());
                    dataMap.put("email", userDataSelect.get("email").toString());
                    dataMap.put("phone", userDataSelect.get("phone").toString());
                    dataMap.put("birthday", userDataSelect.get("birthday").toString());
                    dataMap.put("judge", true);
                }
            } catch (JwtException e) {

                throw new JwtException("JWT 無效", e);
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
    public ResponseEntity<?> saveProfile(LoginSaveProfileRequest request, LoginUser user) {
        final String name = request.getName().trim();
        final String phone = request.getPhone().trim();
        final String birthday = request.getBirthday().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("remark", "修改會員資料失敗");
        dataMap.put("name", name);
        dataMap.put("email", user.email());
        dataMap.put("phone", "");
        dataMap.put("birthday", "");
        dataMap.put("judge", false);
        LoginSaveProfile loginSaveProfile = new LoginSaveProfile();
        loginSaveProfile.setName(name);
        loginSaveProfile.setEmail(user.email());
        loginSaveProfile.setPhone(phone);
        loginSaveProfile.setBirthday(birthday);
        loginMapper.save(loginSaveProfile);
        dataMap.put("remark", "修改會員資料成功");
        dataMap.put("phone", phone);
        dataMap.put("birthday", birthday);
        dataMap.put("judge", true);

        final String userDataOnly = String.format(
                RedisKey.redisUserDataKey.get("userDataOnly"),
                user.email()
        );
        Login login = new Login(user.email());
        Map<String, Object> userDataSelect = loginMapper.select(login);
        String jsonMap = objectMapper.writeValueAsString(userDataSelect);
        stringRedisTemplate.opsForValue().set(
                userDataOnly, jsonMap, Duration.ofSeconds(refreshExpirationSecondsAddRndomNumber()));
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
    public ResponseEntity<?> logout(LoginUser user, String refreshToken) {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap = new TreeMap<>();
        dataMap.put("remark", "登出失敗");
        dataMap.put("judge", false);
        if (StringUtils.hasText(refreshToken)) {
            try {
                Claims claims = jwtTokenService.validateRefreshToken(refreshToken);
                final String jti = claims.getId();
                final String jwt = claims.getSubject();
                final String refreshRedisKey = String.format(
                        RedisKey.redisKey.get("refresh"),
                        jti,
                        jwt
                );
                long remainingMillis = Duration.between(
                        Instant.now(),
                        user.expiresAt()
                ).toMillis();
                long remainingSeconds = remainingMillis > 0
                        ? (remainingMillis + 999) / 1000
                        : 0;
                if (remainingSeconds > 0) {
                    final String blacklistRedisKey = String.format(
                            RedisKey.redisKey.get("blacklist"),
                            jti
                    );
                    stringRedisTemplate.opsForValue().set(
                            blacklistRedisKey,
                            "revoked",
                            Duration.ofSeconds(remainingSeconds)
                    );
                }
                final String refreshJtiRedisKey = String.format(
                        RedisKey.redisKey.get("refreshJti"),
                        jwt
                );
                final String accessRedisKey = String.format(
                        RedisKey.redisKey.get("access"),
                        jti,
                        jwt
                );
                final String userDataOnly = String.format(
                        RedisKey.redisUserDataKey.get("userDataOnly"),
                        jwt
                );
                stringRedisTemplate.delete(refreshRedisKey);
                stringRedisTemplate.delete(refreshJtiRedisKey);
                stringRedisTemplate.delete(accessRedisKey);
                stringRedisTemplate.delete(userDataOnly);
                dataMap.put("remark", "登出成功");
                dataMap.put("judge", true);
            } catch (JwtException e) {

                throw new JwtException("JWT 無效", e);
            }
        }
        data.add(dataMap);
        HttpStatus status = HttpStatus.OK;
        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/v1/login")
                .maxAge(0)
                .build();
        return ResponseEntity
                .status(status)
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

}
