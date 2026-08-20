package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Booking.BookingDopaypriceTicket;
import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import com.demo.ticket.Dto.Booking.SalesDate;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Mapper
public interface BookingMapper {

    List<Map<String, Object>> selectOnlyActivities(String activityName);

    List<Map<String, Object>> selectOnlySession(
            @Param("date") String date,
            @Param("activityName") String activityName
    );

    @MapKey("id")
    Map<String, Map<String, Object>> selectOnlyActivitiesPrice(String activityId);

    BigDecimal selectActivityPrice(String activityName);

    int saveTicket(BookingSaveTicket bookingSaveTicket);

    List<Map<String,Object>> selectOnlyTicket(String customer);

    int cancelTicket(BookingSaveTicket bookingSaveTicket);

    List<Map<String, Object>> sessionSalesDate(SalesDate salesDate);

    int dopaypriceTicket(BookingDopaypriceTicket bookingDopaypriceTicket);
}
