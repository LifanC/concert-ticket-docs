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

    // <App>:<Domain>:<Purpose>:<ID>
    public static final Map<String, String> redisUserDataKey = Map.of(
            "userDataOnly", "userData:map:only:%s"
    );

}
