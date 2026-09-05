package com.demo.ticket.Mapper;

import com.demo.ticket.Dto.Activity.ActivityFavorite;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {

    List<Map<String, Object>> selectAllActivities();

    List<Map<String, Object>> selectOnlyFavoriteActivities(ActivityFavorite activityFavorite);

    int saveFavoriteActivity(ActivityFavorite activityFavorite);

    int deleteFavoriteActivity(ActivityFavorite activityFavorite);

}
