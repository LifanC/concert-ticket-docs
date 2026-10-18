package com.demo.ticket.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.InetAddress;
import java.util.Map;

@NullMarked
public class ClientIpHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servlet) {
            Object address = servlet.getServletRequest().getAttribute(ClientIpFilter.CLIENT_IP);
            if (address instanceof InetAddress) {
                attributes.put(ClientIpFilter.CLIENT_IP, address);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            @Nullable Exception exception) {
    }
}
