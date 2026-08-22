package com.demo.ticket.Service;

import com.demo.ticket.Dto.Admin.*;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Mapper.AdminMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AdminServiceImpl implements AdminService{

    private final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final AdminMapper adminMapper;
    private final JwtTokenService jwtTokenService;

    public AdminServiceImpl(
            AdminMapper adminMapper,
            JwtTokenService jwtTokenService
    ) {
        this.adminMapper = adminMapper;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllActivities(AdminRequest request) {
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data;
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> authorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (Activities)有效的 JWT UUID {}",
                    accessJwt,
                    Arrays.toString(authorities.toArray()),
                    accessJtId
            );
            data = adminMapper.selectAllActivities();
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(Activities)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        return data;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllSessions(AdminRequest request) {
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data;
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> authorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (Sessions)有效的 JWT UUID {}",
                    accessJwt,
                    Arrays.toString(authorities.toArray()),
                    accessJtId
            );
            data = adminMapper.selectAllSessions();
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(Sessions)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        return data;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllticket(AdminRequest request) {
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data;
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> authorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (ticket)有效的 JWT UUID {}",
                    accessJwt,
                    Arrays.toString(authorities.toArray()),
                    accessJtId
            );
            data = adminMapper.selectAllticket();
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(ticket)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        return data;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveActivity(AdminSaveActivityRequest request) {
        final String id = request.getId().trim();
        final String name = request.getName().trim();
        final String category = request.getCategory().trim();
        final String date = request.getDate().trim();
        final String venue = request.getVenue().trim();
        final String activityStatus = request.getStatus().trim();
        final BigDecimal price = request.getPrice();
        final String description = request.getDescription().trim();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data;
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> authorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (管理員後台_新增、修改)有效的 JWT UUID {}",
                    accessJwt,
                    Arrays.toString(authorities.toArray()),
                    accessJtId
            );
            Activity activity = new Activity();
            activity.setId(id);
            activity.setName(name);
            activity.setCategory(category);
            activity.setDate(date);
            activity.setVenue(venue);
            activity.setStatus(activityStatus);
            activity.setPrice(price);
            activity.setDescription(description);
            adminMapper.create_activity(activity);
            data = adminMapper.selectAllActivities();
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(管理員後台_新增、修改)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public ResponseEntity<?> deleteActivity(AdminDeleteActivityRequest request) {
        final String id = request.getId().trim();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data;
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> authorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (管理員後台_刪除)有效的 JWT UUID {}",
                    accessJwt,
                    Arrays.toString(authorities.toArray()),
                    accessJtId
            );
            adminMapper.delete_activity(id);
            data = adminMapper.selectAllActivities();
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(管理員後台_刪除)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public ResponseEntity<?> createSession(AdminCreateSessionRequest request) {
        final String id = request.getId().trim();
        final String activity = request.getActivity().trim();
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        final String salesdate = request.getSalesdate().trim();
        final String salestime = request.getSalestime().trim();
        final BigDecimal capacity = request.getCapacity();
        final BigDecimal sold = request.getSold();
        final String accessToken = request.getToken().trim();
        List<Map<String, Object>> data;
        try {
            Claims accessClaims = jwtTokenService.accessTokenInRedis(accessToken);
            String accessJwt = accessClaims.getSubject();
            List<String> authorities = accessClaims.get("authorities", List.class);
            String accessJtId = accessClaims.getId();
            logger.error("{}(權限{}) : (新增場次)有效的 JWT UUID {}",
                    accessJwt,
                    Arrays.toString(authorities.toArray()),
                    accessJtId
            );
            Session session = new Session();
            session.setId(id);
            session.setActivity(activity);
            session.setDate(date);
            session.setTime(time);
            session.setSalesdate(salesdate);
            session.setSalestime(salestime);
            session.setCapacity(capacity);
            session.setSold(sold);
            adminMapper.create_session(session);
            data = adminMapper.selectAllSessions();
        } catch (JwtException e) {
            // JWT 不合法
            logger.error("(新增場次)無效的 JWT token");
            throw new JwtException("無效的 JWT token", e);
        }
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

}
