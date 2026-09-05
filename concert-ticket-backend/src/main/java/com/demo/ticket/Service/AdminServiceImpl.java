package com.demo.ticket.Service;

import com.demo.ticket.Dto.Admin.*;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Mapper.AdminMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AdminServiceImpl implements AdminService{

    private final AdminMapper adminMapper;

    public AdminServiceImpl(
            AdminMapper adminMapper
    ) {
        this.adminMapper = adminMapper;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllActivities() {
        return adminMapper.selectAllActivities();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllSessions() {
        return adminMapper.selectAllSessions();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllticket() {
        return adminMapper.selectAllticket();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public ResponseEntity<?> saveActivity(AdminSaveActivityRequest request) {
        final String id = request.getId().trim();
        final String name = request.getName().trim();
        final String category = request.getCategory().trim();
        final String venue = request.getVenue().trim();
        final BigDecimal price = request.getPrice();
        final String description = request.getDescription().trim();
        final String column = request.getColumn().trim();
        final BigDecimal row = request.getRow();
        final String seat_id = column + "-" + row.toString();
        Activity activity = new Activity();
        activity.setId(id);
        activity.setName(name);
        activity.setCategory(category);
        activity.setVenue(venue);
        activity.setPrice(price);
        activity.setDescription(description);
        String activity_id = adminMapper.create_activity(activity);
        StringJoiner result = new StringJoiner(", ");
        for (char c = column.charAt(0); c <= column.charAt(1); c++) {
            result.add(String.valueOf(c));
        }
        adminMapper.create_seat(seat_id, activity_id, result.toString(), row);
        List<Map<String, Object>> data = adminMapper.selectAllActivities();
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public ResponseEntity<?> deleteActivity(AdminDeleteActivityRequest request) {
        final String id = request.getId().trim();
        adminMapper.delete_activity(id);
        List<Map<String, Object>> data = adminMapper.selectAllActivities();
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public ResponseEntity<?> createSession(AdminCreateSessionRequest request) {
        final String id = request.getId().trim();
        final String activity_id = request.getActivity_id().trim();
        final String date = request.getDate().trim();
        final String time = request.getTime().trim();
        final String salesdate = request.getSalesdate().trim();
        final String salestime = request.getSalestime().trim();
        final String statusSession = request.getStatus().trim();
        Session session = new Session();
        session.setId(id);
        session.setActivity_id(activity_id);
        session.setDate(date);
        session.setTime(time);
        session.setSalesdate(salesdate);
        session.setSalestime(salestime);
        BigDecimal capacity = BigDecimal.ZERO;
        Map<String, Object> dataMapOnlySeats = adminMapper.selectOnlySeats(activity_id).get(activity_id);
        if (dataMapOnlySeats != null) {
            int rows = dataMapOnlySeats.get("seat_rows").toString().split(",").length;
            int seatsPerRow = Integer.parseInt(dataMapOnlySeats.get("seats_per_row").toString());

            capacity = BigDecimal.valueOf((long) rows * seatsPerRow);
        }
        session.setCapacity(capacity);
        session.setStatus(statusSession);
        adminMapper.create_session(session);
        List<Map<String, Object>> data = adminMapper.selectAllSessions();
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

}
