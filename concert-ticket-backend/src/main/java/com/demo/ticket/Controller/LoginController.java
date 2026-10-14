// 閱讀導引：會員登入及資料的 HTTP 入口；接收與驗證請求後委派服務層執行，並組裝回應。
package com.demo.ticket.Controller;

import com.demo.ticket.Dto.Login.*;
import com.demo.ticket.Service.Login.LoginService;
import com.demo.ticket.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Login API", description = "登入功能")
@RestController
@RequestMapping("/v1/login")
@Validated
public class LoginController {

    private final LoginService loginService;

    /**
     * 注入會員服務，處理註冊、登入、token 更新及會員資料請求。
     */
    public LoginController(
            LoginService loginService
    ){
        this.loginService = loginService;
    }

    /**
     * 建立會員並以 BCrypt 編碼密碼，回傳註冊結果。
     */
    @Operation(summary = "1.註冊", description = "註冊")
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid
            @RequestBody
            RegisterRequest request) {
        return loginService.register(request);
    }

    /**
     * 提交帳號或 email 與密碼；成功時回傳 HttpOnly refresh cookie，前端再呼叫 validate 取得 access token。
     */
    @Operation(summary = "2.登入", description = "登入")
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid
            @RequestBody
            LoginRequest request) {
        return loginService.login(request);
    }

    /**
     * 從 refresh token cookie 取得 token，委派登入服務驗證並回傳新的 access token。
     */
    @Operation(summary = "3.驗證", description = "驗證Token")
    @PostMapping("/validate")
    public ResponseEntity<?> validate(
            @CookieValue(name = "refreshToken")
            String refreshToken) {
        return loginService.validate(refreshToken);
    }

    /**
     * 以登入會員身分提交電話與生日更新；姓名只用於既有會員資料比對，並非修改欄位。
     */
    @Operation(summary = "4.修改會員資料", description = "修改會員資料")
    @PutMapping("/saveProfile")
    public ResponseEntity<?> saveProfile(
            @Valid
            @RequestBody
            LoginSaveProfileRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return loginService.saveProfile(request, user);
    }

    /**
     * 撤銷登入相關 Redis 資料及 token，並清除 refresh token cookie。
     */
    @Operation(summary = "5.登出", description = "登出")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal LoginUser user,
            @CookieValue(name = "refreshToken")
            String refreshToken) {
        return loginService.logout(user, refreshToken);
    }

}
