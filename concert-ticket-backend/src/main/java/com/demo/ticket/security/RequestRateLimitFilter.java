package com.demo.ticket.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@NullMarked
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private final RedisRateLimiter limiter;
    private final RequestLimits limits;

    public RequestRateLimitFilter(RedisRateLimiter limiter, RequestLimits limits) {
        this.limiter = limiter;
        this.limits = limits;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        try {
            checkLimits(request);
        } catch (RateLimitException ex) {
            SecurityErrorResponse.write(
                    response,
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    ex.getRetryAfter(),
                    "請求過於頻繁，請稍後再試"
            );
            return;
        } catch (DataAccessException | IllegalStateException ex) {
            SecurityErrorResponse.write(
                    response,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    5,
                    "服務暫時無法使用"
            );
            return;
        }

        // 只處理限流檢查的例外，後續業務錯誤交給原本的處理流程。
        chain.doFilter(request, response);
    }

    /** 所有請求先檢查共用配額，再依路徑檢查個別配額。 */
    private void checkLimits(HttpServletRequest request) {
        String requestPath = RequestPathUtils.path(request);
        // 只使用實際連線來源，不接受客戶端自行提供的 X-Forwarded-For。
        String clientIp = request.getRemoteAddr();
        int windowSeconds = limits.getWindowSeconds();

        limiter.check("api-ip", clientIp, limits.getApiPerIp(), windowSeconds);

        switch (requestPath) {
            case "/v1/login/login" ->
                    limiter.check("login-ip", clientIp, limits.getLoginPerIp(), windowSeconds);
            case "/v1/login/register" ->
                    limiter.check("register-ip", clientIp, limits.getRegisterPerIp(), windowSeconds);
            case "/v1/login/validate" ->
                    limiter.check("refresh-ip", clientIp, limits.getRefreshPerIp(), windowSeconds);
            default -> {
                if (requestPath.equals("/ws") || requestPath.startsWith("/ws/")) {
                    limiter.check("ws-ip", clientIp, limits.getWsPerIp(), windowSeconds);
                }
            }
        }
    }

}
