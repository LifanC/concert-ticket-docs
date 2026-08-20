package com.demo.ticket.Service;

import com.demo.ticket.Mapper.LoginMapper;
import com.demo.ticket.Mapper.SecretMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceImplTests {

    @Mock
    private SecretMapper secretMapper;
    @Mock
    private LoginMapper loginMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        when(secretMapper.getSecretOnly()).thenReturn("01234567890123456789012345678901");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new JwtTokenServiceImpl(secretMapper, loginMapper, redisTemplate);
    }

    @Test
    void validatesRefreshTokenOnlyWhenItMatchesRedis() {
        String token = service.createRefreshToken("token-id", 1800, "member");
        when(valueOperations.get("userData:jwt:refresh:member")).thenReturn(token);

        assertEquals("member", service.validateRefreshToken(token).getSubject());
        verify(valueOperations).set(eq("userData:jwt:refresh:member"), eq(token), eq(Duration.ofSeconds(1800)));
    }

    @Test
    void rejectsRefreshTokenThatWasRevokedOrReplaced() {
        String token = service.createRefreshToken("token-id", 1800, "member");
        when(valueOperations.get(anyString())).thenReturn("different-token");

        assertThrows(JwtException.class, () -> service.validateRefreshToken(token));
    }
}
