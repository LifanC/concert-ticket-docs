// 閱讀導引：會員登入及資料的服務契約；定義呼叫端可使用的操作，實際流程由對應實作類別負責。
package com.demo.ticket.Service.Login;

import com.demo.ticket.Dto.Login.*;
import com.demo.ticket.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

public interface LoginService {

    /**
     * 建立會員並以 BCrypt 編碼密碼，回傳註冊結果。
     */
    ResponseEntity<?> register(@Valid RegisterRequest request);

    /**
     * 提交帳號或 email 與密碼；成功時回傳 HttpOnly refresh cookie，前端再呼叫 validate 取得 access token。
     */
    ResponseEntity<?> login(@Valid LoginRequest request);

    /**
     * 以 refresh token 更新 access token，回傳登入資料與驗證結果。
     */
    ResponseEntity<?> validate(String refreshToken);

    /**
     * 以登入會員身分提交電話與生日更新；姓名只用於既有會員資料比對，並非修改欄位。
     */
    ResponseEntity<?> saveProfile(@Valid LoginSaveProfileRequest request, LoginUser user);

    /**
     * 撤銷登入相關 Redis 資料及 token，並清除 refresh token cookie。
     */
    ResponseEntity<?> logout(LoginUser user, String refreshToken);
}
