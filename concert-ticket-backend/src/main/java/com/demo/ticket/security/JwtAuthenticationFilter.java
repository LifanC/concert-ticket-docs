package com.demo.ticket.security;

import com.demo.ticket.Common.ConvertFormat;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService
    ) {
        this.jwtTokenService = jwtTokenService;
    }

    private static final Set<String> PUBLIC_PATH_PREFIX = Set.of(
            "/api/v1/login/register",
            "/api/v1/login/login",
            "/api/v1/login/validate",
            "/api/v1/login/logout",
            "/api/v1/activity/selectAllActivities",
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
            HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = ConvertFormat.resolveToken(request.getHeader("Authorization"));
        Claims claims;
        try {
            claims = jwtTokenService.accessTokenInRedis(token);
        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {
                      "status": 401,
                      "message": "JWT 已過期"
                    }
                    """);
            return;

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {
                      "status": 401,
                      "message": "JWT 無效"
                    }
                    """);
            return;
        }
        String account = claims.getSubject();
        List<String> authorities = claims.get("authorities", List.class);
        logger.info("account={}", account);
        logger.info("authorities={}", Arrays.toString(authorities.toArray()));
        List<SimpleGrantedAuthority> grantedAuthorities =
                authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        account,
                        null,
                        grantedAuthorities
                );
        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
        logger.info("authentication after={}", SecurityContextHolder.getContext().getAuthentication());
        filterChain.doFilter(request, response);
    }

}
