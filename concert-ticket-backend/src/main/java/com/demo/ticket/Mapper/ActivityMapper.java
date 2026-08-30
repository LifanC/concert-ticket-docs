package com.demo.ticket.Mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {

    List<Map<String, Object>> selectAllActivities();

    List<Map<String, Object>> selectOnlyFavoriteActivities(String user_email);

    int saveFavoriteActivity(String user_email, String activity_id);

    int deleteFavoriteActivity(String user_email, String activity_id);

}
