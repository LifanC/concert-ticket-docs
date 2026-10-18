package com.demo.ticket;

import com.demo.ticket.Mapper.LoginMapper;
import com.demo.ticket.Mapper.SecretMapper;
import com.demo.ticket.Service.JwtTokenServiceImpl;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtSecurityTests {

    StringRedisTemplate redis;
    ValueOperations<String, String> values;
    JwtTokenServiceImpl tokens;
    String refresh;
    String access;
    final String email = "admin@example.com";

    @BeforeEach
    void setup() {
        redis = mock(StringRedisTemplate.class);
        values = mock();
        when(redis.opsForValue()).thenReturn(values);
        SecretMapper secrets = mock(SecretMapper.class);
        when(secrets.getSecretOnly()).thenReturn("unit-test-signing-key-at-least-32-bytes-long");
        LoginMapper members = mock(LoginMapper.class);
        when(members.selectPermissions(email)).thenReturn(java.util.List.of("USER_ITEM_IMPLEMENT"));
        tokens = new JwtTokenServiceImpl(secrets, members, redis);
        refresh = tokens.createRefreshToken("unused", "session", 120, email);
        access = tokens.createAccessToken(60, "session", email);
        when(values.get("userData:jwt:refresh:jti:" + email)).thenReturn("session");
        when(values.get("userData:jwt:refresh:session:" + email)).thenReturn(refresh);
        when(redis.hasKey("userData:jwt:access:session:" + email)).thenReturn(true);
    }

    @Test
    void validSessionAcceptsBothTokenTypesAtTheirOwnEndpoints() {
        assertEquals(email, tokens.validateRefreshToken(refresh).getSubject());
        assertEquals(email, tokens.accessTokenInRedis(access).getSubject());
    }

    @Test
    void accessCannotBeUsedAsRefresh() {
        assertThrows(JwtException.class, () -> tokens.validateRefreshToken(access));
    }

    @Test
    void refreshCannotBeUsedAsAccess() {
        assertThrows(JwtException.class, () -> tokens.accessTokenInRedis(refresh));
    }

    @Test
    void loggedOutSessionRejectsBothTokensEvenIfOldKeysRemain() {
        when(values.get("userData:jwt:refresh:jti:" + email)).thenReturn(null);
        assertThrows(JwtException.class, () -> tokens.validateRefreshToken(refresh));
        assertThrows(JwtException.class, () -> tokens.accessTokenInRedis(access));
    }

    @Test
    void replacementLoginInvalidatesPreviousSession() {
        when(values.get("userData:jwt:refresh:jti:" + email)).thenReturn("new-session");
        assertThrows(JwtException.class, () -> tokens.validateRefreshToken(refresh));
        assertThrows(JwtException.class, () -> tokens.accessTokenInRedis(access));
    }

    @Test
    void removedRefreshRecordCannotIssueNewAccess() {
        when(values.get("userData:jwt:refresh:session:" + email)).thenReturn(null);
        assertThrows(JwtException.class, () -> tokens.validateRefreshToken(refresh));
    }

    @Test
    void missingAccessRecordRejectsSignedAccess() {
        when(redis.hasKey("userData:jwt:access:session:" + email)).thenReturn(false);
        assertThrows(JwtException.class, () -> tokens.accessTokenInRedis(access));
    }

    @Test
    void blacklistRejectsBothTokenTypes() {
        when(redis.hasKey("userData:jwt:blacklist:session")).thenReturn(true);
        assertThrows(JwtException.class, () -> tokens.validateRefreshToken(refresh));
        assertThrows(JwtException.class, () -> tokens.accessTokenInRedis(access));
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = tokens.createAccessToken(-60, "session", email);
        assertThrows(JwtException.class, () -> tokens.accessTokenInRedis(expired));
    }
}
