package com.demo.ticket;

import com.demo.ticket.Dto.Login.LoginRequest;
import com.demo.ticket.Mapper.LoginMapper;
import com.demo.ticket.Service.JwtTokenService;
import com.demo.ticket.Service.Login.LoginServiceImpl;
import com.demo.ticket.security.LoginUser;
import com.demo.ticket.security.RateLimitException;
import com.demo.ticket.security.RedisRateLimiter;
import com.demo.ticket.security.RequestLimits;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginSecurityTests {

    LoginMapper mapper;
    StringRedisTemplate redis;
    JwtTokenService tokens;
    RedisRateLimiter limiter;
    BCryptPasswordEncoder passwords;
    LoginServiceImpl service;

    @BeforeEach
    void setup() {
        mapper = mock(LoginMapper.class);
        redis = mock(StringRedisTemplate.class);
        tokens = mock(JwtTokenService.class);
        limiter = mock(RedisRateLimiter.class);
        passwords = mock(BCryptPasswordEncoder.class);
        service = new LoginServiceImpl(passwords, mapper, redis, tokens, limiter, new RequestLimits());
        ReflectionTestUtils.setField(service, "accessSeconds", 1800);
    }

    @Test
    void badPasswordDoesNotWriteAnyLoginState() {
        when(mapper.select(any())).thenReturn(Map.of("email", "member@example.com", "password", "hash"));
        when(passwords.matches(anyString(), anyString())).thenReturn(false);
        service.login(new LoginRequest("member", "wrong123"));
        verifyNoInteractions(redis, tokens);
    }

    @Test
    void accountRateLimitStopsBeforePasswordHashWork() {
        doThrow(new RateLimitException(60)).when(limiter).check(eq("login-account"), anyString(), anyInt(), anyInt());
        assertThrows(RateLimitException.class, () -> service.login(new LoginRequest("member", "password123")));
        verifyNoInteractions(mapper, passwords, redis, tokens);
    }

    @Test
    void mismatchedRefreshCannotLogOutAnotherSession() {
        when(tokens.validateRefreshToken("refresh")).thenReturn(
                Jwts.claims().setId("other").setSubject("other@example.com")
        );
        assertThrows(JwtException.class, () -> service.logout(
                new LoginUser("session", "member@example.com", Instant.now(), true), "refresh"
        ));
        verifyNoInteractions(redis);
    }

    @Test
    @NullMarked
    void logoutRaceCannotResurrectSessionWhenIssuingAccess() {
        when(tokens.validateRefreshToken("refresh")).thenReturn(Jwts.claims().setId("session")
                .setSubject("member@example.com").setExpiration(Date.from(Instant.now().plusSeconds(60))));
        when(mapper.select(any())).thenReturn(Map.of("email", "member@example.com"));
        when(tokens.createAccessToken(anyInt(), anyString(), anyString())).thenReturn("access");
        // 若登出或重新登入先完成，原子腳本回傳 0，禁止重新啟用舊 session。
        when(redis.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                any(Object[].class)
        )).thenReturn(0L);
        assertThrows(JwtException.class, () -> service.validate("refresh"));
    }
}
