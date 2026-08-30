package com.demo.ticket.Service;

import com.demo.ticket.Dto.Activity.ActivityFavoriteRequest;
import com.demo.ticket.Dto.Activity.ActivityRequest;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

public interface ActivityService {

    List<Map<String, Object>> selectAllActivities();

    List<Map<String, Object>> selectOnlyFavoriteActivities(ActivityRequest request);

    ResponseEntity<?> saveFavoriteActivity(ActivityFavoriteRequest request);

    ResponseEntity<?> deleteFavoriteActivity(ActivityFavoriteRequest request);

}
