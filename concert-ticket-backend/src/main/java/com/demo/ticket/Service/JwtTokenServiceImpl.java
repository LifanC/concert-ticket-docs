// 閱讀導引：集中處理 JWT 的簽發與驗證，供登入流程及身分驗證使用。
package com.demo.ticket.Service;

import com.demo.ticket.Mapper.LoginMapper;
import com.demo.ticket.Mapper.SecretMapper;
import com.demo.ticket.Common.RedisKey;
import io.jsonwebtoken.JwtException;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private final SecretMapper secretMapper;
    private final LoginMapper loginMapper;
    private final StringRedisTemplate redis;

    /**
     * 注入當月簽章祕密與會員權限查詢介面，供 token 簽發及驗證使用。
     */
    public JwtTokenServiceImpl(
            SecretMapper secretMapper,
            LoginMapper loginMapper,
            StringRedisTemplate redis
    ) {
        this.secretMapper = secretMapper;
        this.loginMapper = loginMapper;
        this.redis = redis;
    }

    /**
     * 讀取資料庫當月的祕密字串，以字串本身的 UTF-8 位元組建立 HMAC 金鑰；方法名稱的 Today 不表示每日輪替。
     */
    private SecretKey getKeyForToday() {
        String secret = secretMapper.getSecretOnly();
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 以 subject、jti 與有效秒數簽發 refresh token，使用資料庫當月祕密建立 HMAC 簽章。
     *
     * @param refreshRedisKey                        保留的介面參數，目前實作未使用；Redis 紀錄由登入服務寫入
     * @param jti                                    此次登入的識別碼
     * @param refreshExpirationSecondsAddRndomNumber 從現在起算的有效秒數
     * @param account                                寫入 subject 的會員 email
     * @return 簽章後的 JWT 字串
     */
    @Override
    public String createRefreshToken(
            String refreshRedisKey,
            String jti,
            int refreshExpirationSecondsAddRndomNumber,
            String account) {
        // JWT 簽名與驗證用的「祕密字串（secret）」
        return Jwts.builder()
                .claim("token_use", "refresh")
                .setId(jti)
                .setSubject(account)
                .setIssuedAt(new Date())
                .setExpiration(
                        Date.from(
                                Instant.now().plus(refreshExpirationSecondsAddRndomNumber, ChronoUnit.SECONDS)
                        )
                )
                .signWith(getKeyForToday())
                .compact();
    }

    /**
     * 驗證 refresh 類型、簽章、期限及 Redis 有效 session 與 token 紀錄。
     */
    @Override
    public Claims validateRefreshToken(String refreshToken) {
        Claims claims = Jwts.parserBuilder()
                .require("token_use", "refresh")
                .setSigningKey(getKeyForToday())  // 你生成 token 時用的密鑰
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();
        requireSession(claims);
        String stored = redis.opsForValue().get(String.format(RedisKey.redisKey.get("refresh"),
                claims.getId(), claims.getSubject()));
        if (!refreshToken.equals(stored)) throw new JwtException("登入已失效");
        return claims;
    }

    /**
     * 查詢會員權限並簽發含 authorities 的 access token，回傳 token 字串。
     */
    @Override
    public String createAccessToken(int accessExpirationSecondsAddRndomNumber, String accessJtId, String accountJwt) {
        List<String> permissions = loginMapper.selectPermissions(accountJwt);
        return Jwts.builder()
                .claim("token_use", "access")
                .setId(accessJtId)
                .setSubject(accountJwt)
                .claim("authorities", permissions)
                .setIssuedAt(new Date())
                .setExpiration(
                        Date.from(
                                Instant.now().plus(accessExpirationSecondsAddRndomNumber, ChronoUnit.SECONDS)
                        )
                )
                .signWith(getKeyForToday())
                .compact();
    }

    /**
     * 驗證 access 類型、簽章、期限、權限與 Redis 有效 session；HTTP 和 WebSocket 共用。
     */
    @Override
    public Claims accessTokenInRedis(String accessToken) {
        Claims claims = Jwts.parserBuilder()
                .require("token_use", "access")
                .setSigningKey(getKeyForToday())  // 你生成 token 時用的密鑰
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
        requireSession(claims);
        if (!Boolean.TRUE.equals(redis.hasKey(String.format(RedisKey.redisKey.get("access"),
                claims.getId(), claims.getSubject())))) {
            throw new JwtException("登入已失效");
        }
        if (!(claims.get("authorities") instanceof List<?> authorities)
                || authorities.stream().anyMatch(item -> !(item instanceof String))) {
            throw new JwtException("Token 權限無效");
        }
        return claims;
    }

    private void requireSession(Claims claims) {
        String sessionId = claims.getId();
        String email = claims.getSubject();

        if (sessionId == null || email == null || claims.getExpiration() == null) {
            throw new JwtException("Token 無效");
        }

        String sessionKey = String.format(RedisKey.redisKey.get("refreshJti"), email);
        String currentSessionId = redis.opsForValue().get(sessionKey);
        if (!sessionId.equals(currentSessionId)) {
            throw new JwtException("登入已失效");
        }

        String blacklistKey = String.format(RedisKey.redisKey.get("blacklist"), sessionId);
        if (Boolean.TRUE.equals(redis.hasKey(blacklistKey))) {
            throw new JwtException("登入已失效");
        }
    }
}
