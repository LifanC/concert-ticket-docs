package com.demo.ticket;

import com.demo.ticket.security.RateLimitException;
import com.demo.ticket.security.RedisRateLimiter;
import com.demo.ticket.security.RequestLimits;
import com.demo.ticket.security.RequestRateLimitFilter;
import com.demo.ticket.security.SecurityConfig;
import com.demo.ticket.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RequestSecurityTests {

    RedisRateLimiter limiter;
    RequestLimits limits;
    RequestRateLimitFilter filter;
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    FilterChain chain;

    @BeforeEach
    void setup() {
        limiter = mock(RedisRateLimiter.class);
        limits = new RequestLimits();
        filter = new RequestRateLimitFilter(limiter, limits);
        request = new MockHttpServletRequest("POST", "/api/v1/login/login");
        request.setContextPath("/api");
        request.setRemoteAddr("192.0.2.1");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @Test
    void loginChecksGlobalAndIpBudgets() throws Exception {
        filter.doFilter(request, response, chain);
        verify(limiter).check("api-ip", "192.0.2.1", 300, 60);
        verify(limiter).check("login-ip", "192.0.2.1", 20, 60);
        verify(chain).doFilter(request, response);
    }

    @Test
    void spoofedForwardedHeaderCannotChangeIdentity() throws Exception {
        request.addHeader("X-Forwarded-For", "203.0.113.9");
        filter.doFilter(request, response, chain);
        verify(limiter).check("login-ip", "192.0.2.1", 20, 60);
    }

    @Test
    void exhaustedBudgetReturns429AndRetryAfterWithoutCallingEndpoint() throws Exception {
        doThrow(new RateLimitException(17)).when(limiter).check("login-ip", "192.0.2.1", 20, 60);
        filter.doFilter(request, response, chain);
        assertEquals(429, response.getStatus());
        assertEquals("17", response.getHeader("Retry-After"));
        verifyNoInteractions(chain);
    }

    @Test
    void crossOriginRateLimitExposesRetryAfterToFrontend() throws Exception {
        SecurityConfig config = new SecurityConfig(mock(JwtAuthenticationFilter.class), filter);
        ReflectionTestUtils.setField(config, "allowedOrigins", new String[]{"http://localhost:5173"});
        CorsFilter cors = new CorsFilter(config.corsConfigurationSource());
        request.addHeader("Origin", "http://localhost:5173");
        doThrow(new RateLimitException(17)).when(limiter).check("login-ip", "192.0.2.1", 20, 60);

        cors.doFilter(request, response, (req, res) -> filter.doFilter(req, res, chain));

        assertEquals(429, response.getStatus());
        assertEquals("17", response.getHeader("Retry-After"));
        assertEquals("Retry-After", response.getHeader("Access-Control-Expose-Headers"));
        assertEquals("http://localhost:5173", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        verifyNoInteractions(chain);
    }

    @Test
    void redisFailureFailsClosedWith503() throws Exception {
        doThrow(new RedisConnectionFailureException("offline"))
                .when(limiter)
                .check(anyString(), anyString(), anyInt(), anyInt());
        filter.doFilter(request, response, chain);
        assertEquals(503, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void preflightDoesNotConsumeBudget() throws Exception {
        request.setMethod("OPTIONS");
        filter.doFilter(request, response, chain);
        verifyNoInteractions(limiter);
        verify(chain).doFilter(request, response);
    }

    @Test
    void downstreamFailureIsNotConvertedIntoAuthenticationOrRateLimitError() {
        assertThrows(jakarta.servlet.ServletException.class, () -> {
            doThrow(new jakarta.servlet.ServletException("business failure")).when(chain).doFilter(request, response);
            filter.doFilter(request, response, chain);
        });
    }
}
