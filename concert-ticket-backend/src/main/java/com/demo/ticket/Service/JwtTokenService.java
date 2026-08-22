package com.demo.ticket.Service;

import io.jsonwebtoken.Claims;

public interface JwtTokenService {

    String createRefreshToken(String refreshRedisKey, String jti, int refreshExpirationSecondsAddRndomNumber, String account);

    Claims validateRefreshToken(String refreshToken);

    String createAccessToken(int accessExpirationSecondsAddRndomNumber, String accessJtId, String accountJwt);

    Claims accessTokenInRedis(String accessToken);
}
