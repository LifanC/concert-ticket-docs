package com.demo.ticket.Service;

import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface BookingService {

    List<Map<String, Object>> selectOnlyActivities(BookingSelectOnlyActivitiesRequest request, LoginUser user);

    List<Map<String, Object>> selectOnlySession(BookingSelectOnlySessionRequest request, LoginUser user);

    List<Map<String, Object>> selectOnlyTicket(LoginUser user);

    Map<String, Object> selectOnlyActivitiesPrice(BookingSelectOnlyActivitiesPriceRequest request, LoginUser user);

    ResponseEntity<?> saveTicket(@Valid BookingSaveTicketRequest request, LoginUser user);

    ResponseEntity<?> cancelOrder(@Valid BookingCanceTicketRequest request, LoginUser user);

    Map<String, Object> sessionSalesDate(@Valid BookingSessionSalesDateRequest request, LoginUser user);

    ResponseEntity<?> dopayprice(@Valid BookingDopaypriceRequest request, LoginUser user);

    List<Map<String, Object>> selectOnlySeats(@Valid BookingSelectOnlySeatsRequest request, LoginUser user);

    List<String> selectOnlyUnavailableSeats(@Valid BookingSelectOnlyUnavailableSeatsRequest request, LoginUser user);
}
