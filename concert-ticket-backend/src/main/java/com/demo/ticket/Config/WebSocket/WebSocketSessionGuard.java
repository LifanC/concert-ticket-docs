package com.demo.ticket.Config.WebSocket;

import com.demo.ticket.Service.JwtTokenService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Component
public class WebSocketSessionGuard implements WebSocketHandlerDecoratorFactory {
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final Map<String, Identity> identities = new ConcurrentHashMap<>();
    private final Map<String, String> subscriptions = new ConcurrentHashMap<>();
    private final JwtTokenService tokens;
    @Value("${security.websocket.max-connections-per-ip:20}")
    private int maxPerIp;
    @Value("${security.websocket.max-connections-per-user:5}")
    private int maxPerUser;
    @Value("${security.websocket.max-connections:1000}")
    private int maxConnections;

    public WebSocketSessionGuard(JwtTokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    @NullMarked
    public WebSocketHandlerDecorator decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                synchronized (connections) {
                    InetAddress sessionIp = remoteIp(session);
                    long count = connections.values().stream()
                            .filter(connection -> Objects.equals(remoteIp(connection.socket()), sessionIp))
                            .count();
                    if (count >= maxPerIp || connections.size() >= maxConnections) {
                        session.close(CloseStatus.POLICY_VIOLATION);
                        return;
                    }
                    connections.put(session.getId(), new Connection(session, System.currentTimeMillis()));
                }
                try {
                    super.afterConnectionEstablished(session);
                } catch (Exception ex) {
                    connections.remove(session.getId());
                    identities.remove(session.getId());
                    subscriptions.remove(session.getId());
                    throw ex;
                }
            }

            @Override
            @NullMarked
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                connections.remove(session.getId());
                identities.remove(session.getId());
                subscriptions.remove(session.getId());
                super.afterConnectionClosed(session, status);
            }
        };
    }

    private @Nullable InetAddress remoteIp(WebSocketSession session) {
        InetSocketAddress remoteAddress = session.getRemoteAddress();
        if (remoteAddress == null) {
            return null;
        }
        return remoteAddress.getAddress();
    }

    public synchronized void authenticate(String sessionId, String token, String email) {
        if (!connections.containsKey(sessionId)) {
            throw new IllegalArgumentException("Unknown connection");
        }
        if (identities.containsKey(sessionId)) {
            throw new IllegalArgumentException("Duplicate CONNECT");
        }
        if (identities.values()
                .stream()
                .filter(i -> i.email().equals(email)).count() >= maxPerUser) {
            throw new IllegalArgumentException("Too many connections");
        }
        identities.put(sessionId, new Identity(token, email));
    }

    public void validate(String sessionId) {
        Identity identity = identities.get(sessionId);
        if (identity == null) {
            throw new IllegalArgumentException("Unauthenticated connection");
        }
        tokens.accessTokenInRedis(identity.token());
    }

    public void subscribe(String sessionId, String subscriptionId) {
        if (subscriptionId == null || subscriptions.putIfAbsent(sessionId, subscriptionId) != null) {
            throw new IllegalArgumentException("Only one notification subscription is allowed");
        }
    }

    public void unsubscribe(String sessionId, String subscriptionId) {
        if (subscriptionId != null) {
            subscriptions.remove(sessionId, subscriptionId);
        }
    }

    @Scheduled(fixedDelay = 5000, scheduler = "bookingTaskScheduler")
    public void closeInvalidSessions() {
        connections.forEach((id, connection) -> {
            try {
                if (identities.containsKey(id)) {
                    validate(id);
                } else if (System.currentTimeMillis() - connection.openedAt() > 10000) {
                    throw new IllegalArgumentException("CONNECT timeout");
                }
            } catch (RuntimeException ex) {
                try {
                    connection.socket().close(CloseStatus.POLICY_VIOLATION);
                } catch (IOException ignored) {
                } finally {
                    connections.remove(id);
                    identities.remove(id);
                    subscriptions.remove(id);
                }
            }
        });
    }
}
