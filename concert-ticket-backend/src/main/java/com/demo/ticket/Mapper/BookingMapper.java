package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Booking.BookingSaveTicket;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BookingMapper {

    List<Map<String, Object>> selectOnlyActivities(String activityName);

    List<Map<String, Object>> selectOnlySession(String date);

    @MapKey("id")
    Map<String, Map<String, Object>> selectOnlyActivitiesPrice(String activityId);

    void saveTicket(BookingSaveTicket bookingSaveTicket);

    List<Map<String,Object>> selectOnlyTicket(String customer);

    int cancelTicket(BookingSaveTicket bookingSaveTicket);
}
