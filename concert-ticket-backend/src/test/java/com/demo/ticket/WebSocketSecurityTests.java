package com.demo.ticket;

import com.demo.ticket.Config.WebSocket.WebSocketConfig;
import com.demo.ticket.Config.WebSocket.WebSocketSessionGuard;
import com.demo.ticket.Service.JwtTokenService;
import com.demo.ticket.security.RedisRateLimiter;
import com.demo.ticket.security.RequestLimits;
import io.jsonwebtoken.JwtException;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@NullMarked
class WebSocketSecurityTests {

    WebSocketSessionGuard guard;
    ChannelInterceptor inbound;
    ChannelInterceptor outbound;
    MessageChannel channel;

    @BeforeEach
    void setup() {
        guard = mock(WebSocketSessionGuard.class);
        channel = mock(MessageChannel.class);
        WebSocketConfig config = new WebSocketConfig(mock(JwtTokenService.class), mock(RedisRateLimiter.class),
                new RequestLimits(), guard);
        inbound = interceptor(config, false);
        outbound = interceptor(config, true);
    }

    private ChannelInterceptor interceptor(WebSocketConfig config, boolean outgoing) {
        ChannelRegistration registration = mock(ChannelRegistration.class);
        if (outgoing) {
            config.configureClientOutboundChannel(registration);
        } else {
            config.configureClientInboundChannel(registration);
        }
        ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        verify(registration).interceptors(captor.capture());
        return captor.getValue();
    }

    private Message<?> frame(StompCommand command, String destination) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(command);
        headers.setSessionId("socket");
        headers.setUser(() -> "member@example.com");
        headers.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    @Test
    void ownNotificationsAreAllowed() {
        assertNotNull(inbound.preSend(frame(StompCommand.SUBSCRIBE, "/user/queue/notifications"), channel));
        verify(guard).validate("socket");
    }

    @Test
    void directQueueAndOtherUserSubscriptionsAreDenied() {
        for (String destination : List.of("/queue/notifications", "/queue/notifications-userother",
                "/user/other@example.com/queue/notifications", "/user/queue/notifications/**")) {
            assertThrows(IllegalArgumentException.class,
                    () -> inbound.preSend(frame(StompCommand.SUBSCRIBE, destination), channel));
        }
    }

    @Test
    void clientsCannotPublishNotifications() {
        assertThrows(IllegalArgumentException.class,
                () -> inbound.preSend(frame(StompCommand.SEND, "/queue/notifications"), channel));
    }

    @Test
    void revokedSessionCannotSubscribe() {
        doThrow(new JwtException("revoked")).when(guard).validate("socket");
        assertThrows(JwtException.class,
                () -> inbound.preSend(frame(StompCommand.SUBSCRIBE, "/user/queue/notifications"), channel));
    }

    @Test
    void revokedSessionDoesNotReceiveMessages() {
        doThrow(new JwtException("revoked")).when(guard).validate("socket");
        assertNull(outbound.preSend(frame(StompCommand.MESSAGE, "/user/queue/notifications"), channel));
    }
}
