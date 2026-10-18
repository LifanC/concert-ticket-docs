package com.demo.ticket.security;

import lombok.Getter;

@Getter
public class RateLimitException extends RuntimeException {
    private final long retryAfter;

    public RateLimitException(long retryAfter) {
        super("請求過於頻繁，請稍後再試");
        this.retryAfter = retryAfter;
    }

}
