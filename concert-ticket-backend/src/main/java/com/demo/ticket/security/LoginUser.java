// 閱讀導引：HTTP 登入身分：保存 token 識別碼、會員 email、期限及 Redis 登入紀錄是否存在；權限另存於 SecurityContext。
package com.demo.ticket.security;

import java.time.Instant;

public record LoginUser(
        String tokenId,
        String email,
        Instant expiresAt,
        Boolean accessExists
) {}
