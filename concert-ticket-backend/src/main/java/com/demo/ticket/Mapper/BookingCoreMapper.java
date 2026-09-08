package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookingCoreMapper {
    BookingSaveTicket findOrder(String orderno);

    void lockSession(String sessionId);

    Map<String, Object> sessionSnapshot(String sessionId);

    void ensureSeat(String sessionId, String seat);

    int reserveSeat(BookingSaveTicket ticket);

    int transitionSeat(String sessionId, String seat, String orderno, String status);

    List<String> expiredOrders();

    List<String> unavailableSeats(String sessionId);

    int claimKey(String email, String key, String hash);

    Map<String, Object> findKey(String email, String key);

    int completeKey(String email, String key, String orderno, String body);
}
