package com.demo.ticket.security;

import java.util.List;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class SecurityConfig {

    private final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // WebSocket 放行
                        .requestMatchers(
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers(
                                "/v1/login/**",
                                "/v1/activity/selectAllActivities"
                        ).permitAll()
                        .requestMatchers(
                                "/v1/activity/selectOnlyFavoriteActivities",
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
                            logger.info("401 URI = {}", request.getRequestURI());
                            logger.info("401 Authentication = {}", authentication);
                            response.sendError(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "尚未登入"
                            );
                        })

                        // 已登入，但權限不足
                        .accessDeniedHandler((request, response, ex) -> {
                            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            logger.info("403 URI = {}", request.getRequestURI());
                            logger.info("403 Authentication = {}", authentication);
                            logger.info("Authorities = {}", authentication != null
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
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
                List.of("*")
        );
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
