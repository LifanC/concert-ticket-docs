// 閱讀導引：從請求解析 JWT 並建立 Spring Security 登入身分，讓後續 API 取得使用者資訊。
package com.demo.ticket.security;

import lombok.extern.slf4j.Slf4j;
import com.demo.ticket.Common.ConvertFormat;
import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_ATTRIBUTE =
            JwtAuthenticationFilter.class.getName() + ".REQUEST_ID";

    private final JwtTokenService jwtTokenService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 注入 JWT 驗證、Redis 登入紀錄與 JSON 回應工具，供每次 HTTP 請求驗證使用。
     */
    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenService = jwtTokenService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    private static final Set<String> PUBLIC_PATH_PREFIX = Set.of(
            "/api/v1/login/register",
            "/api/v1/login/login",
            "/api/v1/login/validate",
            "/api/v1/activity/selectAllActivities",
            "/api/v1/activity/activityImage/",
            // WebSocket
            "/api/ws",
            // Swagger
            "/api/swagger-ui",
            "/api/v3/api-docs"
    );

    /**
     * 用完整請求 URI 的前綴比對公開端點，回傳 true 表示略過 JWT 驗證；同時建立或沿用 requestId 供追蹤。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 不需要 JWT 的路徑
        boolean skip = PUBLIC_PATH_PREFIX.stream().anyMatch(path::startsWith);
        Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        }
        log.info("JWT requestId={} method={} uri={} dispatcher={} alreadyFiltered={} skip={}",
                requestId,
                request.getMethod(),
                path,
                request.getDispatcherType(),
                request.getAttribute(getAlreadyFilteredAttributeName()) != null,
                skip);
        return skip;
    }

    /**
     * 驗證 Bearer JWT 的簽章與期限，拒絕黑名單 token，將權限與 LoginUser 放入 Spring Security 上下文。
     * Redis access 紀錄是否存在保存在 LoginUser.accessExists，並非在此一律拒絕；後續服務依此確認登入有效性。
     * JWT 過期或無效時清除上下文並回傳 JSON 401，避免後續端點使用不合法身分。
     */
    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws IOException {
        String token = ConvertFormat.resolveToken(request.getHeader("Authorization"));
        try {
            Claims claims = jwtTokenService.accessTokenInRedis(token);
            final String jti = claims.getId();
            final String jwt = claims.getSubject();
            final Instant jexpiresAt = claims.getExpiration().toInstant();
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    jti,
                    jwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            LoginUser user = new LoginUser(
                    jti,
                    jwt,
                    jexpiresAt,
                    accessExists
            );
            List<String> authorities = claims.get("authorities", List.class);
            final String blacklistRedisKey = String.format(
                    RedisKey.redisKey.get("blacklist"),
                    user.tokenId()
            );
            Boolean blacklistExists = stringRedisTemplate.hasKey(blacklistRedisKey);
            if (Boolean.TRUE.equals(blacklistExists)) {
                throw new JwtException("JWT 無效");
            }

            List<SimpleGrantedAuthority> grantedAuthorities =
                    authorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            grantedAuthorities
                    );
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(
                    response.getWriter(),
                    Map.of(
                            "status", 401,
                            "message", "JWT 已過期"
                    )
            );

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(
                    response.getWriter(),
                    Map.of(
                            "status", 401,
                            "message", e.getMessage()
                    )
            );
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            // 如果是 WebSocket handshake，直接拒絕
            if (request.getRequestURI().startsWith("/ws")) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "WebSocket JWT 無效或已過期"
                );
            }
        }
    }

}
