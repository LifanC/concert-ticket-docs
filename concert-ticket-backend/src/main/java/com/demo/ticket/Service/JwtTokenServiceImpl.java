package com.demo.ticket.Service;

import com.demo.ticket.Mapper.LoginMapper;
import com.demo.ticket.Mapper.SecretMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private final SecretMapper secretMapper;
    private final LoginMapper loginMapper;

    public JwtTokenServiceImpl(
            SecretMapper secretMapper,
            LoginMapper loginMapper
    ) {
        this.secretMapper = secretMapper;
        this.loginMapper = loginMapper;
    }

    private SecretKey getKeyForToday() {
        String secret = secretMapper.getSecretOnly();
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

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

    @Override
    public Claims validateRefreshToken(String refreshToken) {
        return Jwts.parserBuilder()
                .setSigningKey(getKeyForToday())  // 你生成 token 時用的密鑰
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();
    }

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

    @Override
    public Claims accessTokenInRedis(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(getKeyForToday())  // 你生成 token 時用的密鑰
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
    }

}
