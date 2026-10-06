package com.demo.ticket.Service.Admin;

import com.demo.ticket.Dto.Admin.Quote;
import com.demo.ticket.Dto.Admin.SalesSettingsRequest;

import java.math.BigDecimal;
import java.util.Map;

public interface SalesSettingsService {
    Map<String, Object> readAdmin(String sessionId);

    Map<String, Object> readBooking(String sessionId, String email);

    Map<String, Object> save(String sessionId, SalesSettingsRequest request);

    Quote quote(String sessionId, String seat, String ticketTypeId, String email, BigDecimal defaultPrice);

}
