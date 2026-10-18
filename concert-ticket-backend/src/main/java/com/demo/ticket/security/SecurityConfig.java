// 閱讀導引：設定 HTTP 請求的存取規則及 JWT 驗證鏈；實際操作權限另由服務層檢查。
package com.demo.ticket.security;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequestRateLimitFilter rateLimitFilter;
    @Value("${security.allowed-origins:http://localhost:5173,https://concert-ticket-frontend.onrender.com}")
    private String[] allowedOrigins;

    /**
     * 取得 JWT filter，稍後將它加入 Spring Security 驗證鏈。
     */
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RequestRateLimitFilter rateLimitFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * 設定 JWT filter 的容器註冊方式，避免與安全驗證鏈重複執行。
     */
    @Bean
    @NullMarked
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        // 停用 Servlet 容器的自動註冊，只讓 Spring Security 鏈執行一次 JWT 驗證。
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 建立 HTTP 安全驗證鏈，套用存取規則與 JWT filter。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // HTTP 握手不在此驗 JWT；STOMP CONNECT 的身分由 WebSocketConfig 驗證。
                        .requestMatchers(
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers(
                                "/v1/login/**",
                                "/v1/activity/selectAllActivities",
                                "/v1/activity/activityImage/**"
                        ).permitAll()
                        .requestMatchers(
                                "/v1/activity/selectOnlyFavoriteActivities"
                        )
                        .hasAuthority("USER_ITEM_IMPLEMENT")
                        .requestMatchers(
                                "/v1/activity/saveFavoriteActivity",
                                "/v1/activity/deleteFavoriteActivity"
                        )
                        .hasAuthority("USER_ITEM_IMPLEMENT")
                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Admin API
                        .requestMatchers(
                                "/v1/admin/**"
                        )
                        .hasAuthority("ADMIN_ITEM_IMPLEMENT")
                        .requestMatchers(
                                "/v1/booking/**"
                        )
                        .hasAuthority("USER_ITEM_IMPLEMENT")

                        // 其他全部需要驗證
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception

                        // 沒登入 / 沒 token
                        .authenticationEntryPoint((request, response, ex) -> {
                            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            log.info("401 URI = {}", request.getRequestURI());
                            log.info("401 Authentication = {}", authentication);
                            response.sendError(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "尚未登入"
                            );
                        })

                        // 已登入，但權限不足
                        .accessDeniedHandler((request, response, ex) -> {
                            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            log.info("403 URI = {}", request.getRequestURI());
                            log.info("403 Authentication = {}", authentication);
                            log.info("Authorities = {}", authentication != null
                                    ? authentication.getAuthorities()
                                    : null);

                            response.sendError(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "權限不足"
                            );
                        })
                )
                // JWT Filter 加入 Security Chain
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @NullMarked
    public FilterRegistrationBean<RequestRateLimitFilter> rateLimitRegistration(RequestRateLimitFilter filter) {
        FilterRegistrationBean<RequestRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 允許列出的前端來源攜帶 cookie，開放指定 HTTP 方法及 Authorization、Content-Type 標頭；其餘來源或自訂標頭未列入。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                List.of(allowedOrigins)
        );
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
