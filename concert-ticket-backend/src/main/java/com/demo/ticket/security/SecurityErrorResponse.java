package com.demo.ticket.security;

import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;

/** 提供安全驗證與限流 Filter 共用的錯誤回應。 */
@NullMarked
public final class SecurityErrorResponse {

    private SecurityErrorResponse() {
    }

    public static void write(HttpServletResponse response,
                             int status,
                             long retryAfter,
                             String message) throws IOException {
        response.setHeader("Retry-After", Long.toString(retryAfter));
        write(response, status, message);
    }

    /** 不需要建議等待時間的錯誤，例如登入憑證無效。 */
    public static void write(HttpServletResponse response,
                             int status,
                             String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
