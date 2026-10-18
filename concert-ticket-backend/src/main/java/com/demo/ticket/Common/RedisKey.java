// 閱讀導引：集中定義 Redis 鍵名格式，讓登入資料與 token 黑名單使用一致的命名方式。
package com.demo.ticket.Common;

import java.util.Map;

public class RedisKey {

    // <App>:<Domain>:<Purpose>:<ID>
    public static final Map<String, String> redisKey = Map.of(
            "refresh", "userData:jwt:refresh:%s:%s",
            "refreshJti", "userData:jwt:refresh:jti:%s",
            "access", "userData:jwt:access:%s:%s",
            "blacklist", "userData:jwt:blacklist:%s"
    );

}
