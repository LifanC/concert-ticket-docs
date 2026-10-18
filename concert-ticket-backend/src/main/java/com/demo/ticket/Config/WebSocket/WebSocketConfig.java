package com.demo.ticket.Config.WebSocket;

import com.demo.ticket.Common.ConvertFormat;
import com.demo.ticket.Service.JwtTokenService;
import com.demo.ticket.security.RedisRateLimiter;
import com.demo.ticket.security.RequestLimits;
import io.jsonwebtoken.Claims;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.*;

import java.util.Collections;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtTokenService tokens;
    private final RedisRateLimiter limiter;
    private final RequestLimits limits;
    private final WebSocketSessionGuard guard;
    @Value("${security.allowed-origins:http://localhost:5173,https://concert-ticket-frontend.onrender.com}")
    private String[] allowedOrigins;

    public WebSocketConfig(
            JwtTokenService tokens,
            RedisRateLimiter limiter,
            RequestLimits limits,
            WebSocketSessionGuard guard) {
        this.tokens = tokens;
        this.limiter = limiter;
        this.limits = limits;
        this.guard = guard;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins)
                .addInterceptors(new com.demo.ticket.security.ClientIpHandshakeInterceptor()).withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(16 * 1024).setSendBufferSizeLimit(64 * 1024)
                .setSendTimeLimit(10000).setTimeToFirstMessage(10000).addDecoratorFactory(guard);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            @NullMarked
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    throw new IllegalArgumentException("Missing STOMP headers");
                }
                StompCommand command = accessor.getCommand();
                if (command == StompCommand.CONNECT) {
                    String token = ConvertFormat.resolveToken(
                            accessor.getFirstNativeHeader("Authorization")
                    );
                    if (token == null || token.isBlank()) {
                        throw new IllegalArgumentException("缺少有效登入憑證");
                    }
                    Claims claims = tokens.accessTokenInRedis(token);
                    limiter.check(
                            "ws-connect-user",
                            claims.getSubject(),
                            limits.getWsConnectPerUser(),
                            limits.getWindowSeconds()
                    );
                    guard.authenticate(accessor.getSessionId(), token, claims.getSubject());
                    accessor.setUser(
                            new UsernamePasswordAuthenticationToken(
                                    claims.getSubject(),
                                    null,
                                    Collections.emptyList()
                            )
                    );
                } else if (command != StompCommand.DISCONNECT) {
                    guard.validate(accessor.getSessionId());
                    if (accessor.getUser() == null) {
                        throw new IllegalArgumentException("Missing principal");
                    }
                    limiter.check(
                            "ws-message-user",
                            accessor.getUser().getName(),
                            limits.getWsMessagesPerUser(),
                            limits.getWindowSeconds()
                    );
                    if (command == StompCommand.SUBSCRIBE) {
                        if (!"/user/queue/notifications".equals(accessor.getDestination())) {
                            throw new IllegalArgumentException("Subscription denied");
                        }
                        guard.subscribe(accessor.getSessionId(), accessor.getSubscriptionId());
                    } else if (command == StompCommand.UNSUBSCRIBE) {
                        guard.unsubscribe(accessor.getSessionId(), accessor.getSubscriptionId());
                    } else if (command != null) {
                        throw new IllegalArgumentException("Command denied");
                    }
                }
                return message;
            }
        });
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            @NullMarked
            public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
                SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
                if (accessor.getMessageType() == SimpMessageType.MESSAGE) {
                    try {
                        guard.validate(accessor.getSessionId());
                    } catch (RuntimeException ex) {
                        return null;
                    }
                }
                return message;
            }
        });
    }
}
