package com.demo.ticket.Service;

import com.demo.ticket.Dto.Booking.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface BookingService {

    List<Map<String, Object>> selectOnlyActivities(BookingSelectOnlyActivitiesRequest request);

    List<Map<String, Object>> selectOnlySession(BookingSelectOnlySessionRequest request);

    List<Map<String, Object>> selectOnlyTicket(BookingSelectOnlyTicketRequest request);

    Map<String, Object> selectOnlyActivitiesPrice(BookingSelectOnlyActivitiesPriceRequest request);

    ResponseEntity<?> saveTicket(@Valid BookingSaveTicketRequest request);

    ResponseEntity<?> cancelOrder(@Valid BookingCanceTicketRequest request);

    Map<String, Object> sessionSalesDate(@Valid BookingSessionSalesDateRequest request);

    ResponseEntity<?> dopayprice(@Valid BookingDopaypriceRequest request);

    List<Map<String, Object>> selectOnlySeats(@Valid BookingSelectOnlySeatsRequest request);

    List<String> selectOnlyUnavailableSeats(@Valid BookingSelectOnlyUnavailableSeatsRequest request);
}
