package com.demo.ticket.security;

import com.demo.ticket.Service.JwtTokenService;
import io.jsonwebtoken.Claims;
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
import org.springframework.util.StringUtils;
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
            "/api/v1/admin/selectAllActivities",
            "/api/v1/admin/selectAllticket",
            "/api/v1/activity/selectAllActivities",
            "/api/v1/booking/selectAllActivities",
            "/api/v1/booking/selectOnlySession",
            "/api/v1/booking/selectOnlyActivitiesPrice",
            "/api/v1/booking/selectOnlyTicket",
            // WebSocket
            "/api/ws"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        logger.info("JWT path = {}", path);
        // 不需要 JWT 的路徑
        boolean skip = PUBLIC_PATH_PREFIX.stream().anyMatch(path::startsWith)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
        logger.info("skip filter = {}", skip);
        return skip;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            Claims claims = jwtTokenService.accessTokenInRedis(token);
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
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}