package com.demo.ticket;

import com.demo.ticket.security.ClientIpFilter;
import com.demo.ticket.security.ClientIpHandshakeInterceptor;
import com.demo.ticket.security.RedisRateLimiter;
import com.demo.ticket.security.RequestLimits;
import com.demo.ticket.security.RequestRateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.http.server.ServerHttpResponse;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClientIpFilterTests {
    private static final String SECRET = "a".repeat(64);

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/login/login");
        request.setContextPath("/api");
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("CF-Connecting-IP", "203.0.113.10");
        request.addHeader("X-Forwarded-For", "192.0.2.99");
        request.addHeader("X-Ticket-Client-IP", "198.51.100.20");
        return request;
    }

    private String resolved(boolean render, String configured, MockHttpServletRequest request) throws Exception {
        String[] result = new String[1];
        new ClientIpFilter(render, configured).doFilter(request, new MockHttpServletResponse(),
                (req, res) -> result[0] = req.getRemoteAddr());
        return result[0];
    }

    @Test
    void localRequestsIgnoreAllProxyHeadersEvenWithCorrectSecret() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-Ticket-Proxy-Secret", SECRET);
        assertEquals("10.0.0.2", resolved(false, SECRET, request));
    }

    @Test
    void directRenderRequestsUseCloudflareAndIgnoreUnverifiedClientHeader() throws Exception {
        assertEquals("203.0.113.10", resolved(true, SECRET, request()));
        MockHttpServletRequest forged = request();
        forged.addHeader("X-Ticket-Proxy-Secret", "wrong");
        assertEquals("203.0.113.10", resolved(true, SECRET, forged));
        assertEquals("203.0.113.10", resolved(true, "", request()));
    }

    @Test
    void authenticatedFrontendIpReachesRateLimiterAndWebSocketHandshake() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-Ticket-Proxy-Secret", SECRET);
        RedisRateLimiter limiter = mock(RedisRateLimiter.class);
        RequestRateLimitFilter limits = new RequestRateLimitFilter(limiter, new RequestLimits());
        Map<String, Object> attributes = new HashMap<>();
        new ClientIpFilter(true, SECRET).doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            limits.doFilter(req, res, (r, s) -> {
                new ClientIpHandshakeInterceptor().beforeHandshake(
                        new ServletServerHttpRequest((HttpServletRequest) r),
                        mock(ServerHttpResponse.class), mock(WebSocketHandler.class), attributes);
            });
        });
        verify(limiter).check("login-ip", "198.51.100.20", 20, 60);
        assertEquals(InetAddress.getByName("198.51.100.20"), attributes.get(ClientIpFilter.CLIENT_IP));
    }

    @Test
    void missingOrInvalidCloudflareIpFallsBackToSocketWithoutDnsLookup() throws Exception {
        for (String value : new String[]{"example.com", "203.0.113.1, 203.0.113.2", "999.0.0.1", "fe80::1%eth0"}) {
            MockHttpServletRequest request = request();
            request.removeHeader("CF-Connecting-IP");
            request.addHeader("CF-Connecting-IP", value);
            assertEquals("10.0.0.2", resolved(true, SECRET, request));
        }
        MockHttpServletRequest missing = request();
        missing.removeHeader("CF-Connecting-IP");
        assertEquals("10.0.0.2", resolved(true, SECRET, missing));
    }

    @Test
    void ipv6IsAcceptedAndInvalidAuthenticatedIpFallsBackToCloudflare() throws Exception {
        MockHttpServletRequest request = request();
        request.removeHeader("CF-Connecting-IP");
        request.addHeader("CF-Connecting-IP", "2001:db8::1");
        assertEquals(InetAddress.getByName("2001:db8::1").getHostAddress(), resolved(true, SECRET, request));
        request.addHeader("X-Ticket-Proxy-Secret", SECRET);
        request.removeHeader("X-Ticket-Client-IP");
        request.addHeader("X-Ticket-Client-IP", "invalid");
        assertNotEquals("invalid", resolved(true, SECRET, request));
    }
}
