package com.demo.ticket.Config.WebSocket;

import org.springframework.web.socket.WebSocketSession;

public record Connection(
        WebSocketSession socket,
        long openedAt
) {}
