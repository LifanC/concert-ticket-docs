package com.demo.ticket.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Render's public ingress overwrites CF-Connecting-IP. Never enable for a directly exposed server. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ClientIpFilter extends OncePerRequestFilter {
    public static final String CLIENT_IP = ClientIpFilter.class.getName() + ".address";
    private final boolean render;
    private final String proxySecret;

    public ClientIpFilter(
            @Value("${RENDER:false}") boolean render,
            @Value("${PROXY_SHARED_SECRET:}") String proxySecret) {
        this.render = render;
        this.proxySecret = proxySecret;
    }

    @Override
    @NullMarked
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        InetAddress address = null;
        if (render) {
            String suppliedSecret = request.getHeader("X-Ticket-Proxy-Secret");
            if (proxySecret.length() >= 64 &&
                    suppliedSecret != null &&
                    MessageDigest.isEqual(
                            proxySecret.getBytes(StandardCharsets.UTF_8),
                            suppliedSecret.getBytes(StandardCharsets.UTF_8)
                    )) {
                address = literal(request.getHeader("X-Ticket-Client-IP"));
            }
            if (address == null) {
                address = literal(request.getHeader("CF-Connecting-IP"));
            }
        }
        if (address == null) {
            address = literal(request.getRemoteAddr());
        }
        if (address == null) {
            chain.doFilter(request, response);
            return;
        }
        request.setAttribute(CLIENT_IP, address);
        String clientIp = address.getHostAddress();
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getRemoteAddr() { return clientIp; }
            @Override
            public String getRemoteHost() { return clientIp; }
        }, response);
    }

    private static InetAddress literal(String value) {
        if (value == null || value.length() > 45 ||
                !(value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}") ||
                  (value.contains(":") && value.matches("[0-9a-fA-F:.]+")))) {
            return null;
        }
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException ex) {
            return null;
        }
    }
}
