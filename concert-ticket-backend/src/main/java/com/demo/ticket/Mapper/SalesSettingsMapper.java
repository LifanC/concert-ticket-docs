package com.demo.ticket.Mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SalesSettingsMapper {
    Map<String, Object> session(String sessionId);

    Map<String, Object> settings(String sessionId);

    List<Map<String, Object>> zones(String sessionId);

    List<Map<String, Object>> ticketTypes(String sessionId);

    List<String> seats(String sessionId);

    int occupied(String sessionId);

    long memberQuantity(String sessionId, String email);

    void saveSettings(Map<String, Object> settings);

    void clearSeatZones(String sessionId);

    void deleteZones(String sessionId);

    void deleteTicketTypes(String sessionId);

    void insertZone(Map<String, Object> zone);

    void insertTicketType(Map<String, Object> type);

    void insertSeats(String sessionId, List<String> seats);

    void assignZone(String sessionId, String zoneId, List<String> seats);

    Map<String, Object> seatPrice(String sessionId, String seat, String ticketTypeId);
}
