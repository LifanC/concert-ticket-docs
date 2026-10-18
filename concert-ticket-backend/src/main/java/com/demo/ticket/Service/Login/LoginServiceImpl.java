package com.demo.ticket.Service.Login;

import com.demo.ticket.Common.ConvertFormat;
import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Dto.Login.Login;
import com.demo.ticket.Dto.Login.LoginRequest;
import com.demo.ticket.Dto.Login.LoginSaveProfile;
import com.demo.ticket.Dto.Login.LoginSaveProfileRequest;
import com.demo.ticket.Dto.Login.Register;
import com.demo.ticket.Dto.Login.RegisterRequest;
import com.demo.ticket.Mapper.LoginMapper;
import com.demo.ticket.Service.JwtTokenService;
import com.demo.ticket.security.LoginUser;
import com.demo.ticket.security.RedisRateLimiter;
import com.demo.ticket.security.RequestLimits;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

@Service
@NullMarked
public class LoginServiceImpl implements LoginService {

    @Value("${auth.cookie.secure:true}")
    private boolean refreshCookieSecure;

    @Value("${auth.cookie.sameSite:Lax}")
    private String refreshCookieSameSite;

    @Value("${jwt.refreshExpirationSeconds}")
    private int refreshSeconds;

    @Value("${jwt.accessExpirationSeconds}")
    private int accessSeconds;

    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginMapper loginMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenService jwtTokenService;
    private final RedisRateLimiter rateLimiter;
    private final RequestLimits requestLimits;

    // 每位會員只有一個有效登入，新登入會取代舊登入。
    // Lua 讓兩筆 Redis 寫入一次完成；舊 token 資料會依原本期限自動刪除。
    private static final DefaultRedisScript<Long> LOGIN = new DefaultRedisScript<>("""
            local sessionKey = KEYS[1]
            local refreshTokenKey = KEYS[2]
            local sessionId = ARGV[1]
            local refreshToken = ARGV[2]
            local expirationSeconds = ARGV[3]

            redis.call('SET', sessionKey, sessionId, 'EX', expirationSeconds)
            redis.call('SET', refreshTokenKey, refreshToken, 'EX', expirationSeconds)
            return 1
            """, Long.class);

    // 寫入 access token 前，再次確認登入未被登出、取代或封鎖。
    // 核對與寫入在同一段 Lua 中完成，避免其他請求插入兩個步驟之間。
    private static final DefaultRedisScript<Long> ISSUE_ACCESS = new DefaultRedisScript<>("""
            local sessionKey = KEYS[1]
            local refreshTokenKey = KEYS[2]
            local accessTokenKey = KEYS[3]
            local blacklistKey = KEYS[4]
            local sessionId = ARGV[1]
            local refreshToken = ARGV[2]
            local accessToken = ARGV[3]
            local requestedSeconds = tonumber(ARGV[4])

            if redis.call('GET', sessionKey) ~= sessionId then
                return 0
            end
            if redis.call('GET', refreshTokenKey) ~= refreshToken then
                return 0
            end
            if redis.call('EXISTS', blacklistKey) == 1 then
                return 0
            end

            local sessionSeconds = redis.call('TTL', sessionKey)
            local refreshSeconds = redis.call('TTL', refreshTokenKey)
            local expirationSeconds = math.min(requestedSeconds, sessionSeconds, refreshSeconds)
            if expirationSeconds < 1 then
                return 0
            end

            redis.call('SET', accessTokenKey, accessToken, 'EX', expirationSeconds)
            return 1
            """, Long.class);

    // 只刪除這次登入的資料，避免舊的登出請求誤刪新登入。
    private static final DefaultRedisScript<Long> LOGOUT = new DefaultRedisScript<>("""
            local sessionKey = KEYS[1]
            local refreshTokenKey = KEYS[2]
            local accessTokenKey = KEYS[3]
            local sessionId = ARGV[1]

            if redis.call('GET', sessionKey) ~= sessionId then
                return 0
            end
            redis.call('DEL', sessionKey, refreshTokenKey, accessTokenKey)
            return 1
            """, Long.class);

    public LoginServiceImpl(BCryptPasswordEncoder passwordEncoder,
                            LoginMapper loginMapper,
                            StringRedisTemplate redisTemplate,
                            JwtTokenService jwtTokenService,
                            RedisRateLimiter rateLimiter,
                            RequestLimits requestLimits) {
        this.passwordEncoder = passwordEncoder;
        this.loginMapper = loginMapper;
        this.redisTemplate = redisTemplate;
        this.jwtTokenService = jwtTokenService;
        this.rateLimiter = rateLimiter;
        this.requestLimits = requestLimits;
    }

    /** 註冊：整理輸入、雜湊密碼，再寫入會員資料。 */
    @Override
    public ResponseEntity<?> register(RegisterRequest request) {
        String account = request.account().trim();
        String name = request.name().trim();
        String email = request.email().trim();
        String phone = request.phone().trim();
        String passwordHash = passwordEncoder.encode(request.password().trim());

        Register registration = new Register(account, name, email, phone, passwordHash);
        loginMapper.create(registration);

        Map<String, Object> data = new TreeMap<>();
        data.put("remark", "註冊成功");
        data.put("account", account);
        data.put("name", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("created_date", ConvertFormat.now());
        data.put("updated_date", "");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.api(HttpStatus.CREATED, List.of(data))
                );
    }

    /** 登入：檢查頻率與帳密，成功後建立 session 並回傳 refresh Cookie。 */
    @Override
    public ResponseEntity<?> login(LoginRequest request) {
        String account = request.account().trim();

        // 先限制輸入帳號的請求次數，再查詢會員。
        rateLimiter.check(
                "login-account",
                account.toLowerCase(Locale.ROOT),
                requestLimits.getLoginPerAccount(),
                requestLimits.getWindowSeconds()
        );

        Map<String, Object> member = loginMapper.select(new Login(account));
        if (member == null || member.isEmpty()) {
            return loginResult(account, false, null);
        }

        // 帳號與 email 都能登入，因此再以實際會員 email 共用一份限流配額。
        String email = member.get("email").toString();
        rateLimiter.check(
                "login-member",
                email.toLowerCase(Locale.ROOT),
                requestLimits.getLoginPerAccount(),
                requestLimits.getWindowSeconds()
        );

        String password = request.password().trim();
        String passwordHash = member.get("password").toString();
        if (!passwordEncoder.matches(password, passwordHash)) {
            return loginResult(account, false, null);
        }

        String sessionId = UUID.randomUUID().toString();
        String refreshTokenKey = key("refresh", sessionId, email);
        String refreshToken =
                jwtTokenService.createRefreshToken(
                        refreshTokenKey,
                        sessionId,
                        refreshSeconds,
                        email
                );

        saveLoginSession(sessionId, email, refreshToken);
        return loginResult(account, true, refreshToken);
    }

    /** 更新 access token：確認 refresh token 有效，重新取得會員資料與權限。 */
    @Override
    public ResponseEntity<?> validate(String refreshToken) {
        Claims claims = jwtTokenService.validateRefreshToken(refreshToken);
        String sessionId = claims.getId();
        String email = claims.getSubject();

        rateLimiter.check("refresh-session", sessionId,
                requestLimits.getRefreshPerSession(), requestLimits.getWindowSeconds());

        // 每次讀取目前會員資料，不把密碼雜湊寫入 Redis 快取。
        Map<String, Object> member = loginMapper.select(new Login(email));
        if (member == null || member.isEmpty()) {
            throw new JwtException("登入已失效");
        }

        int expirationSeconds = accessTokenLifetime(claims);
        String accessToken = jwtTokenService.createAccessToken(expirationSeconds, sessionId, email);

        // keys 的順序對應 ISSUE_ACCESS 裡的 KEYS[1] 到 KEYS[4]。
        List<String> keys = List.of(
                key("refreshJti", email),
                key("refresh", sessionId, email),
                key("access", sessionId, email),
                key("blacklist", sessionId)
        );
        Long result = redisTemplate.execute(
                ISSUE_ACCESS,
                keys,
                sessionId, refreshToken, accessToken, Integer.toString(expirationSeconds)
        );
        if (!Long.valueOf(1).equals(result)) {
            throw new JwtException("登入已失效");
        }

        Map<String, Object> data = new TreeMap<>();
        data.put("remark", "驗證成功");
        data.put("judge", true);
        data.put("accessToken", accessToken);
        data.put("name", Objects.toString(member.get("name"), ""));
        data.put("email", Objects.toString(member.get("email"), ""));
        data.put("phone", Objects.toString(member.get("phone"), ""));
        data.put("birthday", Objects.toString(member.get("birthday"), ""));

        return ResponseEntity.ok(ApiResponse.api(HttpStatus.OK, List.of(data)));
    }

    /** 使用後端登入身分決定修改對象；SQL 以姓名與 email 比對，更新電話和生日。 */
    @Override
    public ResponseEntity<?> saveProfile(LoginSaveProfileRequest request, LoginUser user) {
        String name = request.name().trim();
        String phone = request.phone().trim();
        String birthday = request.birthday().trim();
        String email = user.email();

        LoginSaveProfile profile = LoginSaveProfile.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthday(birthday)
                .build();
        loginMapper.save(profile);

        Map<String, Object> data = new TreeMap<>();
        data.put("remark", "修改會員資料成功");
        data.put("judge", true);
        data.put("name", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("birthday", birthday);

        return ResponseEntity.ok(ApiResponse.api(HttpStatus.OK, List.of(data)));
    }

    /** 登出：核對兩種 token 屬於同一次登入，再刪除 Redis 紀錄與瀏覽器 Cookie。 */
    @Override
    public ResponseEntity<?> logout(LoginUser user, String refreshToken) {
        Claims claims = jwtTokenService.validateRefreshToken(refreshToken);
        String sessionId = user.tokenId();
        String email = user.email();

        if (!sessionId.equals(claims.getId()) || !email.equals(claims.getSubject())) {
            throw new JwtException("登入身分不一致");
        }

        List<String> keys = List.of(
                key("refreshJti", email),
                key("refresh", sessionId, email),
                key("access", sessionId, email)
        );
        redisTemplate.execute(LOGOUT, keys, sessionId);

        ResponseCookie expiredCookie = cookie("", 0);
        Map<String, Object> data = Map.of("remark", "登出成功", "judge", true);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(ApiResponse.api(HttpStatus.OK, List.of(data)));
    }

    /** Redis 的 key 與參數順序對應 LOGIN 腳本，回傳 1 表示寫入完成。 */
    private void saveLoginSession(String sessionId, String email, String refreshToken) {
        List<String> keys = List.of(
                key("refreshJti", email),
                key("refresh", sessionId, email)
        );
        Long result = redisTemplate.execute(
                LOGIN,
                keys,
                sessionId, refreshToken, Integer.toString(refreshSeconds)
        );
        if (!Long.valueOf(1).equals(result)) {
            throw new DataAccessResourceFailureException("登入服務暫時無法使用");
        }
    }

    /** Access token 的期限不能超過 refresh token 的剩餘時間。 */
    private int accessTokenLifetime(Claims claims) {
        long refreshRemainingSeconds =
                claims.getExpiration().toInstant().getEpochSecond() - Instant.now().getEpochSecond();
        int expirationSeconds = (int) Math.min(accessSeconds, refreshRemainingSeconds);
        if (expirationSeconds < 1) {
            throw new JwtException("登入已過期");
        }
        return expirationSeconds;
    }

    /** 帳密不符維持 HTTP 200，由前端根據 judge 判斷登入結果。 */
    private ResponseEntity<?> loginResult(String account, boolean success, @Nullable String refreshToken) {
        Map<String, Object> data = new TreeMap<>();
        data.put("remark", success ? "登入成功" : "登入失敗");
        data.put("account", account);
        data.put("judge", success);

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (refreshToken != null) {
            ResponseCookie refreshCookie = cookie(refreshToken, refreshSeconds);
            response.header(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }
        return response.body(ApiResponse.api(HttpStatus.OK, List.of(data)));
    }

    /** 依 RedisKey 定義的格式組合 key，例如 refresh 的 session ID 與 email。 */
    private String key(String type, Object... arguments) {
        return String.format(RedisKey.redisKey.get(type), arguments);
    }

    private ResponseCookie cookie(String value, long seconds) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/v1/login")
                .maxAge(seconds)
                .build();
    }
}
