package com.demo.ticket;

import com.demo.ticket.Config.WebSocket.WebSocketSessionGuard;
import com.demo.ticket.Service.JwtTokenService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketSessionGuardTests {

    JwtTokenService tokens;
    WebSocketSessionGuard guard;
    WebSocketHandler handler;

    @BeforeEach
    void setup() {
        tokens = mock(JwtTokenService.class);
        guard = new WebSocketSessionGuard(tokens);
        ReflectionTestUtils.setField(guard, "maxPerIp", 2);
        ReflectionTestUtils.setField(guard, "maxPerUser", 1);
        ReflectionTestUtils.setField(guard, "maxConnections", 10);
        handler = guard.decorate(mock(WebSocketHandler.class));
    }

    WebSocketSession socket(String id) {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn(id);
        when(socket.getRemoteAddress()).thenReturn(new InetSocketAddress("192.0.2.1", 12345));
        return socket;
    }

    @Test
    void verifiedVisitorIpsHaveSeparateConnectionBudgets() throws Exception {
        for (int i = 1; i <= 3; i++) {
            WebSocketSession session = socket("visitor-" + i);
            when(session.getAttributes()).thenReturn(java.util.Map.of(
                    com.demo.ticket.security.ClientIpFilter.CLIENT_IP,
                    java.net.InetAddress.getByName("203.0.113." + i)));
            handler.afterConnectionEstablished(session);
            org.mockito.Mockito.verify(session, org.mockito.Mockito.never()).close(org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    void connectionCapClosesExcessConnections() throws Exception {
        handler.afterConnectionEstablished(socket("one"));
        handler.afterConnectionEstablished(socket("two"));
        WebSocketSession third = socket("three");
        handler.afterConnectionEstablished(third);
        verify(third).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void memberConnectionCapPreventsParallelSessionsAndReleasesOnClose() throws Exception {
        WebSocketSession first = socket("one");
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(socket("two"));
        guard.authenticate("one", "token", "member@example.com");
        assertThrows(IllegalArgumentException.class, () -> guard.authenticate("two", "token", "member@example.com"));
        handler.afterConnectionClosed(first, CloseStatus.NORMAL);
        assertDoesNotThrow(() -> guard.authenticate("two", "token", "member@example.com"));
    }

    @Test
    void revokedTokenClosesSocketDuringSweep() throws Exception {
        WebSocketSession socket = socket("one");
        handler.afterConnectionEstablished(socket);
        guard.authenticate("one", "token", "member@example.com");
        when(tokens.accessTokenInRedis("token")).thenThrow(new JwtException("revoked"));
        guard.closeInvalidSessions();
        verify(socket).close(CloseStatus.POLICY_VIOLATION);
        assertThrows(IllegalArgumentException.class, () -> guard.validate("one"));
    }

    @Test
    void repeatedSubscriptionsCannotGrowBrokerState() {
        guard.subscribe("one", "subscription-1");
        assertThrows(IllegalArgumentException.class, () -> guard.subscribe("one", "subscription-2"));
        guard.unsubscribe("one", "subscription-1");
        assertDoesNotThrow(() -> guard.subscribe("one", "subscription-2"));
    }
}
