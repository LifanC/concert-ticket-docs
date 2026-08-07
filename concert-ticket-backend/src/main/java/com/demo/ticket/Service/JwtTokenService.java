package com.demo.ticket.Service;

import io.jsonwebtoken.Claims;

public interface JwtTokenService {

    Boolean createRefreshToken(String jti, int refreshExpirationSecondsAddRndomNumber, String account);

    Claims refreshTokenInRedis(String refreshRedisKey);

    String createAccessToken(int accessExpirationSecondsAddRndomNumber, String accessJtId, String accountJwt);

    Claims accessTokenInRedis(String accessToken);
}
