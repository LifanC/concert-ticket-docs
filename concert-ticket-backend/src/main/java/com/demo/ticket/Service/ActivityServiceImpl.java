package com.demo.ticket.Service;

import com.demo.ticket.Common.RedisKey;
import com.demo.ticket.Dto.Activity.ActivityFavoriteRequest;
import com.demo.ticket.Dto.Activity.ActivityRequest;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Mapper.ActivityMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityMapper activityMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenService jwtTokenService;

    public ActivityServiceImpl(
            ActivityMapper activityMapper,
            StringRedisTemplate stringRedisTemplate,
            JwtTokenService jwtTokenService
    ) {
        this.activityMapper = activityMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public List<Map<String, Object>> selectAllActivities() {
        return activityMapper.selectAllActivities();
    }

    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectOnlyFavoriteActivities(ActivityRequest request) {
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            String accessJtId = accessClaims.getId();
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (收藏活動資料) Token 已過期", accessJwt);
            } else {
                data = activityMapper.selectOnlyFavoriteActivities(accessJwt);
            }
        } catch (JwtException e) {
            logger.error("(收藏活動資料)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        return data;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveFavoriteActivity(ActivityFavoriteRequest request) {
        return changeFavoriteActivity(request, true);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public ResponseEntity<?> deleteFavoriteActivity(ActivityFavoriteRequest request) {
        return changeFavoriteActivity(request, false);
    }

    private ResponseEntity<?> changeFavoriteActivity(ActivityFavoriteRequest request, boolean save) {
        final String activity_id = request.getActivity_id().trim();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("judge", false);
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            String accessJtId = accessClaims.getId();
            final String accessRedisKey = String.format(
                    RedisKey.redisKey.get("access"),
                    accessJtId,
                    accessJwt
            );
            Boolean accessExists = stringRedisTemplate.hasKey(accessRedisKey);
            if (Boolean.FALSE.equals(accessExists)) {
                logger.error("{} : (收藏活動) Token 已過期", accessJwt);
            } else {
                int cnt = save
                        ? activityMapper.saveFavoriteActivity(accessJwt, activity_id)
                        : activityMapper.deleteFavoriteActivity(accessJwt, activity_id);
                dataMap.put("judge", cnt > 0);
                data.add(dataMap);
            }
        } catch (JwtException e) {
            logger.error("(收藏活動)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        HttpStatus status = save ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

}
