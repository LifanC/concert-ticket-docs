package com.demo.ticket;

import com.demo.ticket.Service.JwtTokenService;
import com.demo.ticket.security.JwtAuthenticationFilter;
import com.demo.ticket.security.RateLimitException;
import com.demo.ticket.security.RedisRateLimiter;
import com.demo.ticket.security.RequestLimits;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTests {

    private JwtTokenService tokens;
    private RedisRateLimiter limiter;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        tokens = mock(JwtTokenService.class);
        limiter = mock(RedisRateLimiter.class);
        filter = new JwtAuthenticationFilter(tokens, limiter, new RequestLimits());
        request = new MockHttpServletRequest("POST", "/api/v1/booking/saveTicket");
        request.setContextPath("/api");
        request.addHeader("Authorization", "Bearer test");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void validToken() {
        Claims claims = Jwts.claims().setId("session").setSubject("member@example.com")
                .setExpiration(Date.from(Instant.now().plusSeconds(60)));
        claims.put("authorities", List.of("USER_ITEM_IMPLEMENT"));
        when(tokens.accessTokenInRedis("test")).thenReturn(claims);
    }

    @Test
    void revokedSessionIsRejectedByTheSharedTokenService() throws Exception {
        when(tokens.accessTokenInRedis("test")).thenThrow(new JwtException("revoked"));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        assertNull(response.getHeader("Retry-After"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }

    @Test
    void redisFailureReturns503WithoutCallingBusinessCode() throws Exception {
        when(tokens.accessTokenInRedis("test")).thenThrow(new RedisConnectionFailureException("offline"));
        filter.doFilter(request, response, chain);
        assertEquals(503, response.getStatus());
        assertEquals("5", response.getHeader("Retry-After"));
        verifyNoInteractions(chain);
    }

    @Test
    void bookingRateLimitReturns429WithoutEstablishingAuthentication() throws Exception {
        validToken();
        doThrow(new RateLimitException(17)).when(limiter).check("booking-user", "member@example.com", 20, 60);
        filter.doFilter(request, response, chain);
        assertEquals(429, response.getStatus());
        assertEquals("17", response.getHeader("Retry-After"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }

    @Test
    void malformedAuthorizationNeverReachesTokenService() throws Exception {
        request.removeHeader("Authorization");
        request.addHeader("Authorization", "Basic invalid");
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(tokens, limiter, chain);
    }

    @Test
    void businessErrorsAreNotConvertedIntoAuthenticationErrors() throws Exception {
        validToken();
        doThrow(new ServletException("business failure")).when(chain).doFilter(request, response);
        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));
        assertEquals(200, response.getStatus());
    }
}
