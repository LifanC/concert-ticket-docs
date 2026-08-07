package com.demo.ticket.Service;

import com.demo.ticket.Dto.Booking.BookingCanceTicketRequest;
import com.demo.ticket.Dto.Booking.BookingSaveTicketRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface BookingService {

    List<Map<String, Object>> selectOnlyActivities(String activityName);

    List<Map<String, Object>> selectOnlySession(String date);

    List<Map<String, Object>> selectOnlyTicket(String email);

    Map<String, Object> selectOnlyActivitiesPrice(String activityId);

    ResponseEntity<?> saveTicket(@Valid BookingSaveTicketRequest request);

    ResponseEntity<?> cancelOrder(@Valid BookingCanceTicketRequest request);
}
