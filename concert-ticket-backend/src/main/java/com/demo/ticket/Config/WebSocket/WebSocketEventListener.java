// 閱讀導引：監聽 WebSocket 連線生命週期事件，記錄連線與離線資訊。
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

    /**
     * 處理 WebSocket 連線事件並記錄相關資訊。
     */
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

    /**
     * 處理 WebSocket 離線事件並記錄相關資訊。
     */
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
