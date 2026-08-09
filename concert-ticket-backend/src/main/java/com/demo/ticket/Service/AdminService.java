package com.demo.ticket.Service;

import com.demo.ticket.Dto.Admin.AdminCreateSessionRequest;
import com.demo.ticket.Dto.Admin.AdminDeleteActivityRequest;
import com.demo.ticket.Dto.Admin.AdminRequest;
import com.demo.ticket.Dto.Admin.AdminSaveActivityRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AdminService {

    List<Map<String, Object>> selectAllActivities(AdminRequest request);

    List<Map<String, Object>> selectAllSessions(AdminRequest request);

    List<Map<String, Object>> selectAllticket(AdminRequest request);

    ResponseEntity<?> saveActivity(@Valid AdminSaveActivityRequest request);

    ResponseEntity<?> deleteActivity(@Valid AdminDeleteActivityRequest request);

    ResponseEntity<?> createSession(@Valid AdminCreateSessionRequest request);
}
