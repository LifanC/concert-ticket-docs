package com.demo.ticket.security;

import com.demo.ticket.Common.ConvertFormat;
import com.demo.ticket.Service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** 驗證受保護的請求，將登入會員與權限放入 Spring Security。 */
@Component
@NullMarked
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/v1/login/register",
            "/v1/login/login",
            "/v1/login/validate",
            "/v1/activity/selectAllActivities",
            "/ws",
            "/swagger-ui.html",
            "/v3/api-docs"
    );

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/v1/activity/activityImage/",
            "/ws/",
            "/swagger-ui/",
            "/v3/api-docs/"
    );

    private final JwtTokenService tokens;
    private final RedisRateLimiter limiter;
    private final RequestLimits limits;

    public JwtAuthenticationFilter(JwtTokenService tokens,
                                   RedisRateLimiter limiter,
                                   RequestLimits limits) {
        this.tokens = tokens;
        this.limiter = limiter;
        this.limits = limits;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = RequestPathUtils.path(request);
        return "OPTIONS".equals(request.getMethod())
                || PUBLIC_PATHS.contains(requestPath)
                || PUBLIC_PATH_PREFIXES.stream().anyMatch(requestPath::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        try {
            authenticateRequest(request);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            SecurityErrorResponse.write(
                    response,
                    HttpStatus.UNAUTHORIZED.value(),
                    "Token 無效或已過期"
            );
            return;
        } catch (RateLimitException exception) {
            SecurityContextHolder.clearContext();
            SecurityErrorResponse.write(
                    response,
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    exception.getRetryAfter(),
                    "請求過於頻繁，請稍後再試"
            );
            return;
        } catch (DataAccessException exception) {
            SecurityContextHolder.clearContext();
            SecurityErrorResponse.write(
                    response,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    5,
                    "服務暫時無法使用"
            );
            return;
        }

        // 後續業務錯誤交給原本的處理流程，避免被誤判為登入錯誤。
        chain.doFilter(request, response);
    }

    /** JWT 與 Redis 的登入有效性由 JwtTokenService 統一驗證。 */
    private void authenticateRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null) {
            throw new JwtException("缺少登入憑證");
        }

        String accessToken = ConvertFormat.resolveToken(authorizationHeader);
        if (accessToken == null || accessToken.isBlank()) {
            throw new JwtException("登入憑證格式不正確");
        }

        Claims claims = tokens.accessTokenInRedis(accessToken);
        UsernamePasswordAuthenticationToken authentication = createAuthentication(claims);

        if (RequestPathUtils.path(request).equals("/v1/booking/saveTicket")) {
            limiter.check("booking-user", claims.getSubject(),
                    limits.getBookingPerUser(), limits.getWindowSeconds());
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 將已驗證的 JWT 內容轉成 Spring Security 使用的會員身分與權限。 */
    private UsernamePasswordAuthenticationToken createAuthentication(Claims claims) {
        List<?> permissionCodes = claims.get("authorities", List.class);
        if (permissionCodes == null || permissionCodes.stream().anyMatch(code ->
                !(code instanceof String))) {
            throw new JwtException("Token 權限無效");
        }

        List<SimpleGrantedAuthority> authorities = permissionCodes.stream()
                .map(code -> new SimpleGrantedAuthority((String) code))
                .toList();
        LoginUser user = new LoginUser(
                claims.getId(),
                claims.getSubject(),
                claims.getExpiration().toInstant(),
                true
        );
        return new UsernamePasswordAuthenticationToken(user, null, authorities);
    }
}
