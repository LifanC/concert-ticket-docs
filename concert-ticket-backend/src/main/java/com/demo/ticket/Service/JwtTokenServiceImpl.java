// 閱讀導引：集中處理 JWT 的簽發與驗證，供登入流程及身分驗證使用。
package com.demo.ticket.Service;

import com.demo.ticket.Mapper.LoginMapper;
import com.demo.ticket.Mapper.SecretMapper;
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

    /**
     * 注入當月簽章祕密與會員權限查詢介面，供 token 簽發及驗證使用。
     */
    public JwtTokenServiceImpl(
            SecretMapper secretMapper,
            LoginMapper loginMapper
    ) {
        this.secretMapper = secretMapper;
        this.loginMapper = loginMapper;
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
     * @param refreshRedisKey 保留的介面參數，目前實作未使用；Redis 紀錄由登入服務寫入
     * @param jti 此次登入的識別碼
     * @param refreshExpirationSecondsAddRndomNumber 從現在起算的有效秒數
     * @param account 寫入 subject 的會員 email
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
     * 驗證 refresh token 簽章及期限，回傳 claims；不合法時由 JWT 函式庫拋出例外。
     */
    @Override
    public Claims validateRefreshToken(String refreshToken) {
        return Jwts.parserBuilder()
                .setSigningKey(getKeyForToday())  // 你生成 token 時用的密鑰
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();
    }

    /**
     * 查詢會員權限並簽發含 authorities 的 access token，回傳 token 字串。
     */
    @Override
    public String createAccessToken(int accessExpirationSecondsAddRndomNumber, String accessJtId, String accountJwt) {
        List<String> permissions = loginMapper.selectPermissions(accountJwt);
        return Jwts.builder()
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
     * 驗證 access token 的簽章及期限並回傳 claims；此方法本身未查詢 Redis。
     */
    @Override
    public Claims accessTokenInRedis(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(getKeyForToday())  // 你生成 token 時用的密鑰
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
    }

}
