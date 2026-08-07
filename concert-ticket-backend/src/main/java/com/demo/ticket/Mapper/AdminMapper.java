package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Admin.Activity;
import com.demo.ticket.Dto.Admin.Session;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminMapper {

    List<Map<String,Object>> selectAllActivities();

    List<Map<String,Object>> selectAllSessions();

    List<Map<String, Object>> selectAllticket();

    void create_activity(Activity activity);

    void delete_activity(String id);

    void create_session(Session session);
}
