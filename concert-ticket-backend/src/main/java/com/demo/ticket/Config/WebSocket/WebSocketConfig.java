// 閱讀導引：設定 SockJS/STOMP 連線、訊息路由及連線身分驗證，供會員接收即時通知。
package com.demo.ticket.Config.WebSocket;

import com.demo.ticket.Common.ConvertFormat;
import com.demo.ticket.Service.JwtTokenService;
import io.jsonwebtoken.Claims;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;
import java.util.Objects;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenService jwtTokenService;

    private final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    /**
     * 注入 JWT 驗證服務，在 STOMP CONNECT 階段建立 WebSocket 使用者身分。
     */
    public WebSocketConfig(
            JwtTokenService jwtTokenService
    ) {
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 啟用 /queue 訊息代理並設定 /user 個人訊息前綴。
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 註冊 /ws 連線端點，啟用 SockJS 並設定允許的來源模式。
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 安裝 STOMP 入站攔截器，在 CONNECT 階段驗證 token 並綁定會員身分。
     */
    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {

                    /**
                     * 處理入站 STOMP 訊息；CONNECT 時驗證 JWT 並設定使用者，回傳訊息繼續傳遞。
                     */
                    @Override
                    public Message<?> preSend(
                            @NonNull Message<?> message,
                            @NonNull MessageChannel channel) {
                        StompHeaderAccessor accessor =
                                MessageHeaderAccessor.getAccessor(
                                        message,
                                        StompHeaderAccessor.class
                                );
                        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                            String authHeader = accessor.getFirstNativeHeader("Authorization");
                            logger.info("Authorization = {}", authHeader);
                            String accessToken = ConvertFormat.resolveToken(authHeader);
                            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
                            // 從 JWT 取得 email
                            String accessJwt = accessClaims.getSubject();
                            logger.info("JWT email = {}", accessJwt);
                            // 建立目前 WebSocket 使用者
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            accessJwt,
                                            null,
                                            Collections.emptyList()
                                    );

                            accessor.setUser(authentication);
                            logger.info(
                                    "設定 WebSocket Principal = {}",
                                    Objects.requireNonNull(accessor.getUser()).getName()
                            );
                        }
                        return message;
                    }

                }
        );
    }

}
