package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Admin.Activity;
import com.demo.ticket.Dto.Admin.Session;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminMapper {

    List<Map<String,Object>> selectAllActivities();

    List<Map<String,Object>> selectAllSessions();

    List<Map<String, Object>> selectAllticket();

    String create_activity(Activity activity);

    void delete_activity(String id);

    void create_session(Session session);

    void create_seat(String id, String activity_id, String seat_rows, BigDecimal seats_per_row);

    @MapKey("activity_id")
    Map<String, Map<String, Object>> selectOnlySeats(String activity_id);
}
