package com.demo.ticket.Service;

import com.demo.ticket.Mapper.ActivityMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;

    public ActivityServiceImpl(
            ActivityMapper activityMapper
    ) {
        this.activityMapper = activityMapper;
    }

    @Override
    public List<Map<String, Object>> selectAllActivities() {
        return activityMapper.selectAllActivities();
    }

}
