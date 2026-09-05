package com.demo.ticket.Controller;

import com.demo.ticket.Dto.Login.*;
import com.demo.ticket.Service.LoginService;
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

    public LoginController(
            LoginService loginService
    ){
        this.loginService = loginService;
    }

    @Operation(summary = "1.註冊", description = "註冊")
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid
            @RequestBody
            RegisterRequest request) {
        return loginService.register(request);
    }

    @Operation(summary = "2.登入", description = "登入")
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid
            @RequestBody
            LoginRequest request) {
        return loginService.login(request);
    }

    @Operation(summary = "3.驗證", description = "驗證Token")
    @PostMapping("/validate")
    public ResponseEntity<?> validate(
            @CookieValue(name = "refreshToken", required = false)
            String refreshToken) {
        return loginService.validate(refreshToken);
    }

    @Operation(summary = "4.修改會員資料", description = "修改會員資料")
    @PutMapping("/saveProfile")
    public ResponseEntity<?> saveProfile(
            @Valid
            @RequestBody
            LoginSaveProfileRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return loginService.saveProfile(request, user);
    }

    @Operation(summary = "5.登出", description = "登出")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal LoginUser user,
            @CookieValue(name = "refreshToken", required = false)
            String refreshToken) {
        return loginService.logout(user, refreshToken);
    }

}
