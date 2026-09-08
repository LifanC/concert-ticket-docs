package com.demo.ticket.Service;

import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Mapper.BookingMapper;
import com.demo.ticket.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.time.Instant;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Run only against a disposable PostgreSQL database; recreates the fixture schema. */
@EnabledIfEnvironmentVariable(named = "BOOKING_TEST_JDBC_URL", matches = ".+")
class BookingTransactionTest {
    private JdbcTemplate jdbc;
    private BookingService service;
    private BookingExpirationService expiration;
    private NotifierConsumer notifier;
    private BookingPaymentScheduler scheduler;
    private BookingMapper mapper;
    private com.demo.ticket.Mapper.BookingCoreMapper core;
    private BookingOrderService orders;
    private final LoginUser user = new LoginUser("test", "member@example.com", Instant.now().plusSeconds(600), true);

    @BeforeEach
    void setUp() throws Exception {
        var ds = new DriverManagerDataSource(System.getenv("BOOKING_TEST_JDBC_URL"), "booking_test", "booking_test");
        jdbc = new JdbcTemplate(ds);
        assertEquals("booking_transaction_test", jdbc.queryForObject("SELECT current_database()", String.class),
                "Only the disposable booking_transaction_test database may be used");
        jdbc.execute("DROP SCHEMA IF EXISTS interviewworks_ticket CASCADE");
        jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of("../db-init/init.sql")));
        jdbc.update("INSERT INTO interviewworks_ticket.activity (id, name, category, venue, price) VALUES ('A1', 'Concert', 'MUSIC_CONCERT', 'Hall', 100)");
        jdbc.update("INSERT INTO interviewworks_ticket.seat VALUES ('layout', 'A1', 'A,B', 99)");
        jdbc.update("INSERT INTO interviewworks_ticket.session (id, activity_id, date, time, salesdate, salestime, capacity, reserved, sold, status) VALUES ('S1', 'A1', '2026-10-01', '19:30', '2026-01-01', '00:00', 2, 1, 0, 'TICKETS_ARE_ON_SALE')");
        jdbc.update("INSERT INTO interviewworks_ticket.ticket (orderno, session_id, email, name, date, time, seat, quantity, status, expires_at) VALUES ('O1', 'S1', ?, 'Concert', '2026-10-01', '19:30', 'B-01', 1, 'PENDING_PAYMENT', CURRENT_TIMESTAMP + INTERVAL '10 minutes')", user.email());
        jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of("../db-init/zz-booking-integrity.sql")));
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setMapperLocations(new ClassPathResource("Mapper/BookingMapper.xml"));
        factory.getObject().getConfiguration().addMapper(com.demo.ticket.Mapper.BookingCoreMapper.class);
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(BookingMapper.class);
        core = new SqlSessionTemplate(factory.getObject()).getMapper(com.demo.ticket.Mapper.BookingCoreMapper.class);
        var redis = mock(StringRedisTemplate.class);
        when(redis.hasKey(anyString())).thenReturn(false);
        notifier = mock(NotifierConsumer.class);
        scheduler = mock(BookingPaymentScheduler.class);
        var tx = new DataSourceTransactionManager(ds);
        orders = proxy(new BookingOrderService(mapper, core), tx);
        service = proxy(new BookingServiceImpl(mapper, redis, notifier, scheduler, core, orders), tx);
        expiration = proxy(new BookingExpirationService(orders), tx);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(T target, DataSourceTransactionManager tx) {
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(tx, new AnnotationTransactionAttributeSource()));
        return (T) factory.getProxy();
    }

    private void pay(LoginUser actor) {
        service.dopayprice(new BookingDopaypriceRequest("O1", "S1", "A1", "PENDING_PAYMENT", null, null), actor);
    }

    private void cancel(LoginUser actor) {
        service.cancelOrder(new BookingCanceTicketRequest("O1", "S1", "PENDING_PAYMENT"), actor);
    }

    private BookingSaveTicket ticket() {
        var ticket = new BookingSaveTicket();
        ticket.setOrderno("O1");
        ticket.setSession_id("S1");
        return ticket;
    }

    private void assertState(String status, int reserved, int sold) {
        assertEquals(status, jdbc.queryForObject("SELECT status FROM interviewworks_ticket.ticket WHERE orderno = 'O1'", String.class));
        assertEquals(reserved, jdbc.queryForObject("SELECT reserved FROM interviewworks_ticket.session WHERE id = 'S1'", Integer.class));
        assertEquals(sold, jdbc.queryForObject("SELECT sold FROM interviewworks_ticket.session WHERE id = 'S1'", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT capacity FROM interviewworks_ticket.session WHERE id = 'S1'", Integer.class));
    }

    @Test
    void repeatedPaymentChangesInventoryOnceAndCannotBeCancelled() {
        doAnswer(call -> { assertState("PAID", 0, 1); return null; }).when(notifier).sendNotification(any());
        pay(user);
        assertThrows(BookingException.class, () -> pay(user));
        assertThrows(BookingException.class, () -> cancel(user));
        assertFalse(expiration.expire(ticket()));
        assertState("PAID", 0, 1);
        verify(notifier, times(1)).sendNotification(any());
        verify(scheduler, times(1)).cancelExpiration("O1");
    }

    @Test
    void repeatedCancellationReleasesOnceAndPreservesQuantity() {
        cancel(user);
        assertThrows(BookingException.class, () -> cancel(user));
        assertThrows(BookingException.class, () -> pay(user));
        assertState("CANCELLED", 0, 0);
        assertEquals(1, jdbc.queryForObject("SELECT quantity FROM interviewworks_ticket.ticket", Integer.class));
        verify(notifier, times(1)).sendNotification(any());
    }

    @Test
    void paymentRollsBackWhenInventoryUpdateFails() {
        jdbc.update("UPDATE interviewworks_ticket.session SET reserved = 0");
        assertEquals("INVENTORY_CONSISTENCY_ERROR", assertThrows(BookingException.class, () -> pay(user)).getCode());
        assertState("PENDING_PAYMENT", 0, 0);
        assertNull(jdbc.queryForObject("SELECT paid_at FROM interviewworks_ticket.ticket", Object.class));
        verifyNoInteractions(notifier, scheduler);
    }

    @Test
    void cancellationRollsBackWhenInventoryUpdateFails() {
        jdbc.update("UPDATE interviewworks_ticket.session SET reserved = 0");
        assertThrows(BookingException.class, () -> cancel(user));
        assertState("PENDING_PAYMENT", 0, 0);
        assertNull(jdbc.queryForObject("SELECT cancelled_at FROM interviewworks_ticket.ticket", Object.class));
        verifyNoInteractions(notifier, scheduler);
    }

    @Test
    void otherMemberAndWrongSessionCannotChangeOrder() {
        var stranger = new LoginUser("other", "member@other-domain.com", user.expiresAt(), true);
        assertThrows(BookingException.class, () -> pay(stranger));
        assertThrows(BookingException.class, () -> cancel(stranger));
        assertThrows(BookingException.class, () -> service.cancelOrder(new BookingCanceTicketRequest("O1", "S2", "PENDING_PAYMENT"), user));
        assertState("PENDING_PAYMENT", 1, 0);
        verifyNoInteractions(notifier, scheduler);
    }

    @Test
    void expirationUsesDeadlineAndReleasesOnlyOnce() {
        assertFalse(expiration.expire(ticket()));
        jdbc.update("UPDATE interviewworks_ticket.ticket SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'");
        assertThrows(BookingException.class, () -> pay(user));
        assertTrue(expiration.expire(ticket()));
        assertFalse(expiration.expire(ticket()));
        assertState("EXPIRED", 0, 0);
    }

    @Test
    void expirationRollsBackWhenInventoryUpdateFails() {
        jdbc.update("UPDATE interviewworks_ticket.ticket SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'");
        jdbc.update("UPDATE interviewworks_ticket.session SET reserved = 0");
        assertThrows(BookingException.class, () -> expiration.expire(ticket()));
        assertState("PENDING_PAYMENT", 0, 0);
    }

    @Test
    void reservationStopsAtCapacity() {
        var session = new BookingSession();
        session.setSession_id("S1");
        assertEquals(1, mapper.updateSession(session));
        assertEquals(0, mapper.updateSession(session));
        assertState("PENDING_PAYMENT", 2, 0);
    }

    @Test
    void failedInsertRollsBackReservationAndSequence() {
        jdbc.execute("ALTER TABLE interviewworks_ticket.ticket ADD CONSTRAINT test_insert_failure CHECK (name <> 'Reject')");
        jdbc.update("UPDATE interviewworks_ticket.activity SET name = 'Reject'");
        // The fixture CHECK constraint injects a failure in the actual INSERT statement.
        var request = new BookingSaveTicketRequest("S1", "A1", "Reject", "2026-10-01", "19:30", "PENDING_PAYMENT", "A-01", BigDecimal.TEN);
        assertThrows(RuntimeException.class, () -> service.saveTicket(request, user, java.util.UUID.randomUUID().toString()));
        assertState("PENDING_PAYMENT", 1, 0);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM interviewworks_ticket.ticket_sequence", Integer.class));
        verifyNoInteractions(notifier, scheduler);
    }

    @Test
    void successfulBookingReservesAndUsesServerPriceAndState() {
        var request = new BookingSaveTicketRequest("S1", "A1", "Concert", "2026-10-01", "19:30", "PAID", "A-01", BigDecimal.TEN);
        service.saveTicket(request, user, java.util.UUID.randomUUID().toString());
        assertState("PENDING_PAYMENT", 2, 0);
        assertEquals("PENDING_PAYMENT", jdbc.queryForObject("SELECT status FROM interviewworks_ticket.ticket WHERE orderno <> 'O1'", String.class));
        assertEquals(0, new BigDecimal("100").compareTo(jdbc.queryForObject("SELECT price FROM interviewworks_ticket.ticket WHERE orderno <> 'O1'", BigDecimal.class)));
        verify(notifier).sendNotification(any());
        verify(scheduler).scheduleExpiration(eq(user.email()), any());
    }

    private BookingSaveTicketRequest request(String seat) {
        return new BookingSaveTicketRequest("S1", "A1", "Concert", "2026-10-01", "19:30", "PENDING_PAYMENT", seat, BigDecimal.TEN);
    }

    @Test
    void oneHundredConcurrentRequestsForSameSeatHaveOneWinner() throws Exception {
        jdbc.update("UPDATE interviewworks_ticket.session SET capacity = 200");
        var ready = new java.util.concurrent.CountDownLatch(100);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(100)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 100; i++) {
                final String key = "concurrent-" + i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        service.saveTicket(request("A-01"), user, key);
                        return true;
                    } catch (org.springframework.dao.DuplicateKeyException ex) {
                        return false;
                    } catch (BookingException ex) {
                        assertEquals("SEAT_ALREADY_RESERVED", ex.getCode());
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            int winners = 0;
            for (var future : futures) if (future.get(60, java.util.concurrent.TimeUnit.SECONDS)) winners++;
            assertEquals(1, winners);
        }
        assertEquals(2, jdbc.queryForObject("SELECT reserved FROM interviewworks_ticket.session", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM interviewworks_ticket.ticket WHERE seat = 'A-01'", Integer.class));
    }

    @Test
    void idempotencyReplaysOriginalResponseAndRejectsDifferentSeat() throws Exception {
        var first = service.saveTicket(request("A-01"), user, "retry-key");
        var replay = service.saveTicket(request("A-01"), user, "retry-key");
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        assertEquals(json.readTree(json.writeValueAsString(first.getBody())), json.readTree((String) replay.getBody()));
        assertEquals("IDEMPOTENCY_CONFLICT", assertThrows(BookingException.class,
                () -> service.saveTicket(request("A-02"), user, "retry-key")).getCode());
        assertState("PENDING_PAYMENT", 2, 0);
        verify(notifier, times(1)).sendNotification(any());
    }

    @Test
    void cancellationReleasesSeatAndPaymentMarksItSold() {
        cancel(user);
        assertFalse(core.unavailableSeats("S1").contains("B-01"));
        service.saveTicket(request("B-01"), user, "rebook");
        String orderno = jdbc.queryForObject("SELECT orderno FROM interviewworks_ticket.ticket WHERE status = 'PENDING_PAYMENT'", String.class);
        orders.transition(orderno, "S1", user.email(), com.demo.ticket.Dto.Booking.OrderStatus.PAID);
        assertEquals("SOLD", jdbc.queryForObject("SELECT status FROM interviewworks_ticket.session_seat WHERE seat_id = 'B-01'", String.class));
    }

    @Test
    void recoveryFindsOverdueOrdersWithoutInMemoryTasks() {
        jdbc.update("UPDATE interviewworks_ticket.ticket SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'");
        new BookingExpirationRecovery(core, orders).recover();
        new BookingExpirationRecovery(core, orders).recover();
        assertState("EXPIRED", 0, 0);
        assertFalse(core.unavailableSeats("S1").contains("B-01"));
    }

    @Test
    void invalidSeatRollsBackOrderInventoryAndIdempotencyKey() {
        assertThrows(BookingException.class, () -> service.saveTicket(request("Z-99"), user, "bad-seat"));
        assertState("PENDING_PAYMENT", 1, 0);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM interviewworks_ticket.booking_idempotency", Integer.class));
        verifyNoInteractions(notifier, scheduler);
    }

    @Test
    void differentSeatsCanBeBookedByConcurrentRequests() throws Exception {
        jdbc.update("UPDATE interviewworks_ticket.session SET capacity = 20");
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(10)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<Object>>();
            for (int i = 1; i <= 10; i++) {
                String seat = "A-" + String.format("%02d", i);
                tasks.add(() -> service.saveTicket(request(seat), user, seat));
            }
            for (var future : executor.invokeAll(tasks)) future.get();
        }
        assertEquals(11, jdbc.queryForObject("SELECT reserved FROM interviewworks_ticket.session", Integer.class));
    }

    @Test
    void concurrentSameKeyCreatesOneOrder() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(10)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<Object>>();
            for (int i = 0; i < 10; i++) tasks.add(() -> service.saveTicket(request("A-01"), user, "shared-key"));
            for (var future : executor.invokeAll(tasks)) future.get();
        }
        assertState("PENDING_PAYMENT", 2, 0);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM interviewworks_ticket.booking_idempotency", Integer.class));
        verify(notifier, times(1)).sendNotification(any());
    }

    @Test
    void paymentAndRecoveryCompetingAtDeadlineKeepOneFinalState() throws Exception {
        jdbc.update("UPDATE interviewworks_ticket.ticket SET expires_at = CURRENT_TIMESTAMP + INTERVAL '100 milliseconds'");
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            var payment = executor.submit(() -> {
                start.await();
                try { pay(user); } catch (BookingException ex) { assertEquals("INVALID_ORDER_STATE", ex.getCode()); }
                return null;
            });
            var expiry = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 10; i++) {
                    expiration.expire(ticket());
                    Thread.sleep(25);
                }
                return null;
            });
            start.countDown();
            payment.get(10, java.util.concurrent.TimeUnit.SECONDS);
            expiry.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        String status = jdbc.queryForObject("SELECT status FROM interviewworks_ticket.ticket", String.class);
        assertTrue(java.util.Set.of("PAID", "EXPIRED").contains(status));
        assertState(status, 0, "PAID".equals(status) ? 1 : 0);
        assertEquals("PAID".equals(status) ? "SOLD" : "AVAILABLE",
                jdbc.queryForObject("SELECT status FROM interviewworks_ticket.session_seat WHERE seat_id = 'B-01'", String.class));
    }

    @Test
    void databaseRejectsNegativeAndExcessInventory() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE interviewworks_ticket.session SET reserved = -1"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE interviewworks_ticket.session SET sold = 2"));
        assertState("PENDING_PAYMENT", 1, 0);
    }
}
