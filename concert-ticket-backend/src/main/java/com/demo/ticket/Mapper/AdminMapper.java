package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Admin.Activity;
import com.demo.ticket.Dto.Admin.Session;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper
public interface AdminMapper {

    List<Map<String,Object>> selectAllActivities();

    List<Map<String,Object>> selectAllSessions();

    List<Map<String, Object>> selectAllticket();

    Map<String, Object> create_activity(Activity activity);

    void upsertActivityImage(UUID activity_uuid, String filename, int width, int height, byte[] image_data);

    void deleteActivityImage(String activityId);

    Map<String, Object> selectActivityImage(String activityId);

    void delete_activity(String id);

    void create_session(Session session);

    void create_seat(String id, String activity_id, String seat_rows, BigDecimal seats_per_row);

    void delete_seat_by_activity(String activity_id);

    @MapKey("activity_id")
    Map<String, Map<String, Object>> selectOnlySeats(String activity_id);
}
