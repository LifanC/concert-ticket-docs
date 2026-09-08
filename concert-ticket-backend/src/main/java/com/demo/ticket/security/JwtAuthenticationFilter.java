package com.demo.ticket.security;

import com.demo.ticket.Common.ConvertFormat;
import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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
            "/api/v1/activity/selectOnlyFavoriteActivities",
            // WebSocket
            "/api/ws",
            // Swagger
            "/api/swagger-ui",
            "/api/v3/api-docs"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 不需要 JWT 的路徑
        boolean skip = PUBLIC_PATH_PREFIX.stream().anyMatch(path::startsWith);
        logger.info("JWT path = {}", path);
        logger.info("skip filter = {}", skip);
        return skip;
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {
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
            logger.info("account={}", user.email());
            logger.info("authorities={}", Arrays.toString(authorities.toArray()));
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
            logger.info("authentication after={}", SecurityContextHolder.getContext().getAuthentication());
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
