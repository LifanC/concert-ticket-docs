package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookingCoreMapper {
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    @Select("SELECT orderno, session_id, email, seat, status, expires_at FROM interviewworks_ticket.ticket WHERE orderno = #{orderno}")
    @Results({@Result(column="session_id", property="session_id"), @Result(column="expires_at", property="expires_at")})
    BookingSaveTicket findOrder(String orderno);

    @Select("SELECT id FROM interviewworks_ticket.session WHERE id = #{sessionId} FOR UPDATE")
    String lockSession(String sessionId);

    @Select("SELECT s.id, s.activity_id, s.date, s.time, a.name, a.price FROM interviewworks_ticket.session s JOIN interviewworks_ticket.activity a ON a.id = s.activity_id WHERE s.id = #{sessionId}")
    Map<String, Object> sessionSnapshot(String sessionId);

    @Insert("""
        INSERT INTO interviewworks_ticket.session_seat(session_id, seat_id)
        SELECT DISTINCT s.id, #{seat}
        FROM interviewworks_ticket.session s JOIN interviewworks_ticket.seat layout ON layout.activity_id = s.activity_id
        CROSS JOIN LATERAL unnest(string_to_array(layout.seat_rows, ',')) r(label)
        CROSS JOIN LATERAL generate_series(1, least(layout.seats_per_row, 99)::int) n(num)
        WHERE s.id = #{sessionId} AND trim(r.label) || '-' || lpad(n.num::text, 2, '0') = #{seat}
        ON CONFLICT DO NOTHING
        """)
    int ensureSeat(@Param("sessionId") String sessionId, @Param("seat") String seat);

    @Update("""
        UPDATE interviewworks_ticket.session_seat SET status = 'RESERVED', reserved_by_order = #{orderno},
            reserved_until = #{expires_at}, version = version + 1
        WHERE session_id = #{session_id} AND seat_id = #{seat} AND status = 'AVAILABLE'
        """)
    int reserveSeat(BookingSaveTicket ticket);

    @Update("""
        UPDATE interviewworks_ticket.session_seat SET status = #{status},
            reserved_by_order = CASE WHEN #{status} = 'SOLD' THEN #{orderno} ELSE NULL END,
            reserved_until = NULL, version = version + 1
        WHERE session_id = #{sessionId} AND seat_id = #{seat} AND reserved_by_order = #{orderno} AND status = 'RESERVED'
        """)
    int transitionSeat(@Param("sessionId") String sessionId, @Param("seat") String seat,
                       @Param("orderno") String orderno, @Param("status") String status);

    @Select("SELECT orderno FROM interviewworks_ticket.ticket WHERE status = 'PENDING_PAYMENT' AND expires_at <= CURRENT_TIMESTAMP ORDER BY expires_at, orderno LIMIT 100")
    List<String> expiredOrders();

    @Select("SELECT seat_id FROM interviewworks_ticket.session_seat WHERE session_id = #{sessionId} AND status <> 'AVAILABLE' ORDER BY seat_id")
    List<String> unavailableSeats(String sessionId);

    @Insert("INSERT INTO interviewworks_ticket.booking_idempotency(email, idempotency_key, request_hash) VALUES(#{email}, #{key}, #{hash}) ON CONFLICT DO NOTHING")
    int claimKey(@Param("email") String email, @Param("key") String key, @Param("hash") String hash);

    @Select("SELECT request_hash, orderno, response_body FROM interviewworks_ticket.booking_idempotency WHERE email = #{email} AND idempotency_key = #{key} FOR UPDATE")
    Map<String, Object> findKey(@Param("email") String email, @Param("key") String key);

    @Update("UPDATE interviewworks_ticket.booking_idempotency SET orderno = #{orderno}, response_body = #{body} WHERE email = #{email} AND idempotency_key = #{key} AND orderno IS NULL")
    int completeKey(@Param("email") String email, @Param("key") String key, @Param("orderno") String orderno, @Param("body") String body);
}
