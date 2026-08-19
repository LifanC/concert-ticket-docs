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

    public WebSocketConfig(
            JwtTokenService jwtTokenService
    ) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {

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
