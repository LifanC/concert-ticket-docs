package com.demo.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.demo.ticket.Config.SecretKey;
import com.demo.ticket.Service.BookingExpirationRecovery;
import com.demo.ticket.Service.BookingOrderService;
import org.springframework.aop.support.AopUtils;
import org.springframework.security.web.SecurityFilterChain;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.servlet.Filter;
import com.demo.ticket.Service.JwtTokenService;
import com.demo.ticket.Service.AdminService;
import com.demo.ticket.Service.BookingService;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 測試，陸續開發啟用。
@SpringBootTest(properties = "analytics.enabled=false")
class TicketApplicationTests {
    @Autowired ApplicationContext context;
    // Prevent startup tasks from modifying the developer's database or exporting reports.
    @MockitoBean SecretKey secretKey;
    @MockitoBean BookingExpirationRecovery expirationRecovery;
    @MockitoBean JwtTokenService tokens;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean AdminService admin;
    @MockitoBean BookingService booking;
    @Autowired WebApplicationContext webContext;
    MockMvc mvc;

    @BeforeEach
    void configureHttp() {
        mvc = MockMvcBuilders.webAppContextSetup(webContext)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    void authenticate(String authority) {
        var claims = Jwts.claims().setId("test-token").setSubject("member@example.test")
                .setExpiration(new Date(System.currentTimeMillis() + 60000));
        claims.put("authorities", List.of(authority));
        when(tokens.accessTokenInRedis("test")).thenReturn(claims);
        when(redis.hasKey(anyString())).thenReturn(false);
        when(redis.hasKey("userData:jwt:access:test-token:member@example.test")).thenReturn(true);
    }

    @Test
    void anonymousBookingIsUnauthorized() throws Exception {
        when(tokens.accessTokenInRedis(isNull())).thenThrow(new JwtException("Missing token"));
        mvc.perform(get("/api/v1/booking/selectOnlyTicket").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(booking);
    }

    @Test
    void memberCannotReadAdminData() throws Exception {
        authenticate("USER_ITEM_IMPLEMENT");
        mvc.perform(get("/api/v1/admin/selectAllActivities").contextPath("/api")
                .header("Authorization", "Bearer test")).andExpect(status().isForbidden());
        verifyNoInteractions(admin);
    }

    @Test
    void adminCanReadAdminData() throws Exception {
        authenticate("ADMIN_ITEM_IMPLEMENT");
        mvc.perform(get("/api/v1/admin/selectAllActivities").contextPath("/api")
                .header("Authorization", "Bearer test")).andExpect(status().isOk());
        verify(admin).selectAllActivities();
    }

    @Test
    void memberCanReadOwnTickets() throws Exception {
        authenticate("USER_ITEM_IMPLEMENT");
        mvc.perform(get("/api/v1/booking/selectOnlyTicket").contextPath("/api")
                .header("Authorization", "Bearer test")).andExpect(status().isOk());
        verify(booking).selectOnlyTicket(argThat(user -> user.email().equals("member@example.test")));
    }

    @Test
    void contextLoads() {
        assertNotNull(context.getBean(SecurityFilterChain.class));
        assertTrue(AopUtils.isAopProxy(context.getBean(BookingOrderService.class)),
                "Order transitions must be wrapped in Spring's transaction proxy");
        assertFalse(context.containsBean("salesAnalyticsScheduler"));
    }

}
