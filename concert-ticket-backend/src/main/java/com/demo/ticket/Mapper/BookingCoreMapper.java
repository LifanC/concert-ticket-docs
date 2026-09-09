package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Booking.BookingCompleteKey;
import com.demo.ticket.Dto.Booking.BookingCoreKey;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.BookingTransitionSeat;
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

    int transitionSeat(BookingTransitionSeat bookingTransitionSeat);

    List<String> expiredOrders();

    List<String> unavailableSeats(String sessionId);

    int claimKey(BookingCoreKey bookingCoreKey);

    Map<String, Object> findKey(BookingCoreKey bookingCoreKey);

    int completeKey(BookingCompleteKey bookingCompleteKey);
}
