package com.demo.ticket.security;

import java.time.Instant;

public record LoginUser(
        String tokenId,
        String email,
        Instant expiresAt,
        Boolean accessExists
) {}
