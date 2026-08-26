package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Booking.BookingDopaypriceTicket;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.BookingSalesDate;
import com.demo.ticket.Dto.Booking.BookingSession;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Mapper
public interface BookingMapper {

    List<Map<String, Object>> selectOnlyActivities(String activity_id);

    List<Map<String, Object>> selectOnlySession(String date, String activity_id);

    List<Map<String,Object>> selectOnlyTicket(String customer);

    @MapKey("id")
    Map<String, Map<String, Object>> selectOnlyActivitiesPrice(String activity_id);

    int updateSession(BookingSession bookingSession);

    @MapKey("id")
    Map<String, Map<String, Object>> selectOnlySessionId(String session_id);

    BigDecimal selectActivityPrice(String activity_id);

    void saveTicket(BookingSaveTicket bookingSaveTicket);

    int cancelSession(BookingSession bookingSession);

    int cancelTicket(BookingSaveTicket bookingSaveTicket);

    @MapKey("id")
    Map<String, Map<String, Object>> sessionSalesDate(BookingSalesDate bookingSalesDate);

    int dopaypriceUpdateSession(BookingSession bookingSession);

    int dopaypriceTicket(BookingDopaypriceTicket bookingDopaypriceTicket);
}
