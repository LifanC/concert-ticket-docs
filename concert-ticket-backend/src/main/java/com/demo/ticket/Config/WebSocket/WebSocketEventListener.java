package com.demo.ticket.Config.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger =
            LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        logger.info(
                "WebSocket CONNECT sessionId={}, user={}",
                accessor.getSessionId(),
                accessor.getUser() != null
                        ? accessor.getUser().getName()
                        : null
        );
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {

        logger.info(
                "WebSocket DISCONNECT sessionId={}, user={}",
                event.getSessionId(),
                event.getUser() != null
                        ? event.getUser().getName()
                        : null
        );
    }
}
