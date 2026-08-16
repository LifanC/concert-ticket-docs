package com.demo.ticket.Config.WebSocket;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.springframework.web.socket.messaging.SessionConnectedEvent;

import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

@Component
public class WebSocketEventListener {

    private final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SimpUserRegistry simpUserRegistry;

    public WebSocketEventListener(
            SimpUserRegistry simpUserRegistry
    ) {
        this.simpUserRegistry = simpUserRegistry;
    }

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        Principal user = event.getUser();
        logger.info(
                "WebSocket connected user = {}",
                user != null ? user.getName() : "null"
        );
        logger.info(
                "WebSocket users = {}",
                simpUserRegistry.getUsers()
                        .stream()
                        .map(SimpUser::getName)
                        .toList()
        );
    }
}
