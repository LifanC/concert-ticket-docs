// 閱讀導引：以 MockMvc 驗證 JWT 與 API 權限，並確認訂單服務有交易代理；登入、Redis 與業務服務使用測試替身。
package com.demo.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.demo.ticket.Config.log.SecretKey;
import com.demo.ticket.Service.Booking.BookingExpirationRecovery;
import com.demo.ticket.Service.Booking.BookingOrderService;
import org.springframework.aop.support.AopUtils;
import org.springframework.security.web.SecurityFilterChain;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.servlet.Filter;
import com.demo.ticket.Service.JwtTokenService;
import com.demo.ticket.Service.Admin.AdminService;
import com.demo.ticket.Service.Booking.BookingService;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "analytics.enabled=false")
class TicketApplicationTests {
    @Autowired ApplicationContext context;
    // 使用測試替身停用啟動時的金鑰寫入及逾期掃描；報表排程由測試屬性關閉。
    @MockitoBean SecretKey secretKey;
    @MockitoBean BookingExpirationRecovery expirationRecovery;
    @MockitoBean JwtTokenService tokens;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean AdminService adminService;
    @MockitoBean BookingService booking;
    @Autowired WebApplicationContext webContext;
    MockMvc mvc;

    /**
     * 初始化 MockMvc 測試環境並套用 Spring Security 設定。
     */
    @BeforeEach
    void configureHttp() {
        mvc = MockMvcBuilders.webAppContextSetup(webContext)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    /**
     * 建立具有指定權限的測試登入資訊，供後續 HTTP 測試使用。
     */
    void authenticate(String authority) {
        var claims = Jwts.claims().setId("test-token").setSubject("luke@admin.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60000));
        claims.put("authorities", List.of(authority));
        when(tokens.accessTokenInRedis("test")).thenReturn(claims);
        when(redis.hasKey(anyString())).thenReturn(false);
        when(redis.hasKey("userData:jwt:access:test-token:luke@admin.com")).thenReturn(true);
    }

    /**
     * 驗證匿名使用者無法呼叫訂票 API，且不會觸發訂票服務。
     */
    @Test
    void anonymousBookingIsUnauthorized() throws Exception {
        when(tokens.accessTokenInRedis(isNull())).thenThrow(new JwtException("Missing token"));
        mvc.perform(get("/api/v1/booking/selectOnlyTicket").contextPath("/api"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(booking);
    }

    /**
     * 驗證一般會員無法讀取管理資料。
     */
    @Test
    void memberCannotReadAdminData() throws Exception {

        authenticate("USER_ITEM_IMPLEMENT");

        mvc.perform(get("/api/v1/admin/selectAllActivities")
                .contextPath("/api")
                .header("Authorization", "Bearer test"))
                .andDo(print())
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminService);
    }

    /**
     * 驗證管理員權限能讀取管理 API 並呼叫對應服務。
     */
    @Test
    void adminCanReadAdminData() throws Exception {
        // ① 準備：模擬有管理員權限的登入資料
        authenticate("ADMIN_ITEM_IMPLEMENT");

        // ② 操作：模擬發送 GET 請求
        mvc.perform(get("/api/v1/admin/selectAllActivities")
                        .contextPath("/api")
                        .header("Authorization", "Bearer test"))
                // ③ 檢查：HTTP 狀態碼必須是 200
                .andDo(print())
                .andExpect(status().isOk());

        // ④ 檢查：確實呼叫了管理員服務
        verify(adminService).selectAllActivities();
    }

    /**
     * 驗證會員能查詢自己的票券，並傳入正確登入資訊。
     */
    @Test
    void memberCanReadOwnTickets() throws Exception {
        authenticate("USER_ITEM_IMPLEMENT");
        mvc.perform(get("/api/v1/booking/selectOnlyTicket")
                .contextPath("/api")
                .header("Authorization", "Bearer test"))
                .andDo(print())
                .andExpect(status().isOk());
        verify(booking).selectOnlyTicket(argThat(user -> user.email().equals("luke@admin.com")));
    }

    /**
     * 確認安全驗證鏈存在、訂單服務經過 Spring 交易代理，且 analytics.enabled=false 時不建立報表排程。
     */
    @Test
    void contextLoads() {
        assertNotNull(context.getBean(SecurityFilterChain.class));
        assertTrue(AopUtils.isAopProxy(context.getBean(BookingOrderService.class)),
                "Order transitions must be wrapped in Spring's transaction proxy");
        assertFalse(context.containsBean("salesAnalyticsScheduler"));
    }

}
