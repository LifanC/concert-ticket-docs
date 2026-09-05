package com.demo.ticket.Service;

import com.demo.ticket.Dto.Activity.ActivityFavoriteRequest;
import com.demo.ticket.security.LoginUser;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

public interface ActivityService {

    List<Map<String, Object>> selectAllActivities();

    List<Map<String, Object>> selectOnlyFavoriteActivities(LoginUser user);

    ResponseEntity<?> saveFavoriteActivity(ActivityFavoriteRequest request, LoginUser user);

    ResponseEntity<?> deleteFavoriteActivity(ActivityFavoriteRequest request, LoginUser user);

}
