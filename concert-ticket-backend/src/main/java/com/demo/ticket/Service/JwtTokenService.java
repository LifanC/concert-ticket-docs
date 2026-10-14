// 閱讀導引：JwtToken的服務契約；定義呼叫端可使用的操作，實際流程由對應實作類別負責。
package com.demo.ticket.Service;

import io.jsonwebtoken.Claims;

public interface JwtTokenService {

    /**
     * 以 subject、jti 與有效秒數簽發 refresh token，使用資料庫當月祕密建立 HMAC 簽章。
     * @param refreshRedisKey 保留的介面參數，目前實作未使用；Redis 紀錄由登入服務寫入
     * @param jti 此次登入的識別碼
     * @param refreshExpirationSecondsAddRndomNumber 從現在起算的有效秒數
     * @param account 寫入 subject 的會員 email
     * @return 簽章後的 JWT 字串
     */
    String createRefreshToken(String refreshRedisKey, String jti, int refreshExpirationSecondsAddRndomNumber, String account);

    /**
     * 驗證 refresh token 簽章及期限，回傳 claims；不合法時由 JWT 函式庫拋出例外。
     */
    Claims validateRefreshToken(String refreshToken);

    /**
     * 查詢會員權限並簽發含 authorities 的 access token，回傳 token 字串。
     */
    String createAccessToken(int accessExpirationSecondsAddRndomNumber, String accessJtId, String accountJwt);

    /**
     * 驗證 access token 的簽章及期限並回傳 claims；此方法本身未查詢 Redis。
     */
    Claims accessTokenInRedis(String accessToken);
}
