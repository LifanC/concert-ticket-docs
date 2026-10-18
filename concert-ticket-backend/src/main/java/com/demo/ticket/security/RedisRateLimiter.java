package com.demo.ticket.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@NullMarked
public class RedisRateLimiter {

    // 使用 Lua 一次完成計數與到期時間設定，避免同時請求造成資料不一致。
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
            local n = redis.call('INCR', KEYS[1])
            if n == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            if n > tonumber(ARGV[1]) then
                return math.max(1, redis.call('TTL', KEYS[1]))
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String scope, String identity, int maximum, int seconds) {
        if (maximum < 1 || seconds < 1) {
            throw new IllegalStateException("Invalid rate limit configuration");
        }

        long retry = redis.execute(
                SCRIPT,
                List.of("security:limit:" + scope + ":" + hash(identity)),
                Integer.toString(maximum),
                Integer.toString(seconds)
        );

        if (retry > 0) {
            throw new RateLimitException(retry);
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest
                                    .getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8))
                    );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
