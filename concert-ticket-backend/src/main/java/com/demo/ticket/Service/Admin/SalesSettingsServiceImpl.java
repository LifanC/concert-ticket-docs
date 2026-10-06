package com.demo.ticket.Service.Admin;

import com.demo.ticket.Dto.Admin.Quote;
import com.demo.ticket.Dto.Admin.SalesSettingsRequest;
import com.demo.ticket.Dto.Admin.ZoneRequest;
import com.demo.ticket.Dto.Admin.TicketTypeRequest;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Mapper.BookingCoreMapper;
import com.demo.ticket.Mapper.SalesSettingsMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class SalesSettingsServiceImpl implements SalesSettingsService {
    private final SalesSettingsMapper salesSettingsMapper;
    private final BookingCoreMapper bookingCoreMapper;

    public SalesSettingsServiceImpl(SalesSettingsMapper salesSettingsMapper, BookingCoreMapper bookingCoreMapper) {
        this.salesSettingsMapper = salesSettingsMapper;
        this.bookingCoreMapper = bookingCoreMapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public Map<String, Object> readAdmin(String sessionId) {
        return readSettings(sessionId, null);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public Map<String, Object> readBooking(String sessionId, String email) {
        return readSettings(sessionId, email);
    }

    private Map<String, Object> readSettings(String sessionId, String email) {
        Map<String, Object> session = requireSession(sessionId);
        Map<String, Object> saved = salesSettingsMapper.settings(sessionId);
        Map<String, Object> layout = saved == null ? session : saved;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("configured", saved != null);
        result.put("version", saved == null ? 0L : saved.get("version"));
        result.put("maxTicketsPerMember", saved == null ? null : saved.get("max_tickets_per_member"));
        result.put("rowLabels", rows(layout));
        result.put("seatsPerRow", integer(layout.get("seats_per_row")));
        result.put("defaultPrice", session.get("price"));
        result.put("capacity", session.get("capacity"));
        result.put("locked", isLocked(sessionId, session));
        List<Map<String, Object>> types = saved == null ? List.of() : salesSettingsMapper.ticketTypes(sessionId);
        List<Map<String, Object>> zones = saved == null ? List.of() : salesSettingsMapper.zones(sessionId);
        for (Map<String, Object> zone : zones) {
            Map<String, BigDecimal> prices = new LinkedHashMap<>();
            for (Map<String, Object> type : types)
                prices.put(type.get("id").toString(),
                        calculatePrice(
                                new BigDecimal(zone.get("price").toString()),
                                new BigDecimal(type.get("pricePercent").toString())
                        )
                );
            zone.put("prices", prices);
        }
        result.put("zones", zones);
        result.put("ticketTypes", types);
        if (email != null) {
            long used = salesSettingsMapper.memberQuantity(sessionId, email);
            result.put("memberTicketQuantity", used);
            result.put("remainingAllowance",
                    saved == null
                            ? null
                            : Math.max(0, integer(saved.get("max_tickets_per_member")) - used)
            );
        }
        return result;
    }

    @Transactional
    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public Map<String, Object> save(String sessionId, SalesSettingsRequest request) {
        bookingCoreMapper.lockSession(sessionId);
        Map<String, Object> session = requireSession(sessionId);
        Map<String, Object> saved = salesSettingsMapper.settings(sessionId);
        long version = saved == null ? 0 : ((Number) saved.get("version")).longValue();
        if (request.version() != version)
            throw new BookingException("SETTINGS_VERSION_CONFLICT", "設定已由其他管理員更新，請重新載入", HttpStatus.CONFLICT);
        if (isLocked(sessionId, session)) {
            throw new BookingException("SALES_SETTINGS_LOCKED", "場次已有保留或售出座位，不能修改配置", HttpStatus.CONFLICT);
        }
        Map<String, Object> layout = saved == null ? session : saved;
        List<String> rows = rows(layout);
        int perRow = integer(layout.get("seats_per_row"));
        validate(request, rows, perRow, ((Number) session.get("capacity")).longValue());
        List<String> allSeats = seats(rows, perRow);
        if (!new HashSet<>(allSeats).containsAll(salesSettingsMapper.seats(sessionId)))
            throw invalid("場次既有座位與配置不一致");
        salesSettingsMapper.saveSettings(
                Map.of(
                        "sessionId", sessionId,
                        "version", version + 1,
                        "maxTicketsPerMember", request.maxTicketsPerMember(),
                        "seatRows", String.join(",", rows),
                        "seatsPerRow", perRow)
        );
        salesSettingsMapper.clearSeatZones(sessionId);
        salesSettingsMapper.deleteZones(sessionId);
        salesSettingsMapper.deleteTicketTypes(sessionId);
        salesSettingsMapper.insertSeats(sessionId, allSeats);
        int index = 0;
        for (ZoneRequest zone : request.zones()) {
            String id = UUID.randomUUID().toString();
            salesSettingsMapper.insertZone(
                    Map.of("id", id,
                            "sessionId", sessionId,
                            "name", zone.name().trim(),
                            "color", zone.color(),
                            "rowStart", zone.rowStart(),
                            "rowEnd", zone.rowEnd(),
                            "price", zone.price(),
                            "sortOrder", index++)
            );
            salesSettingsMapper.assignZone(
                    sessionId,
                    id,
                    seats(rows.subList(rows.indexOf(zone.rowStart()), rows.indexOf(zone.rowEnd()) + 1), perRow)
            );
        }
        index = 0;
        for (TicketTypeRequest type : request.ticketTypes()) {
            salesSettingsMapper.insertTicketType(
                    Map.of(
                            "id", UUID.randomUUID().toString(),
                            "sessionId", sessionId,
                            "name", type.name().trim(),
                            "pricePercent", type.pricePercent(),
                            "eligibility", type.eligibility() == null ? "" : type.eligibility().trim(),
                            "sortOrder", index++)
            );
        }
        return readSettings(sessionId, null);
    }

    // Called by saveTicket after it obtains the shared session lock, inside the booking transaction.
    @Override
    public Quote quote(String sessionId, String seat, String ticketTypeId, String email, BigDecimal defaultPrice) {
        Map<String, Object> settings = salesSettingsMapper.settings(sessionId);
        if (settings == null) {
            if (ticketTypeId != null && !ticketTypeId.isBlank()) throw invalid("此場次尚未設定票種");
            if (defaultPrice == null || defaultPrice.signum() < 0) throw invalid("活動票價設定無效");
            return new Quote(
                    defaultPrice,
                    null,
                    null,
                    null,
                    null
            );
        }
        if (salesSettingsMapper.memberQuantity(sessionId, email) >= integer(settings.get("max_tickets_per_member"))) {
            throw new BookingException("PURCHASE_LIMIT_REACHED", "已達此場次限購張數（含有效待付款訂單）", HttpStatus.CONFLICT);
        }
        if (ticketTypeId == null || ticketTypeId.isBlank()) throw invalid("請選擇票種");
        Map<String, Object> selected = salesSettingsMapper.seatPrice(sessionId, seat, ticketTypeId);
        if (selected == null) throw invalid("座位或票種不屬於此場次");
        BigDecimal price = new BigDecimal(selected.get("price").toString());
        BigDecimal percent = new BigDecimal(selected.get("price_percent").toString());
        return new Quote(
                calculatePrice(price, percent),
                selected.get("zone_id").toString(),
                selected.get("ticket_type_id").toString(),
                selected.get("zone_name").toString(),
                selected.get("ticket_type_name").toString()
        );
    }

    private BigDecimal calculatePrice(BigDecimal price, BigDecimal percent) {
        return price.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private void validate(SalesSettingsRequest request, List<String> rows, int perRow, long capacity) {
        validateLayout(rows, perRow, capacity);
        validateConfiguration(request);
        validateZones(request.zones(), rows);
        validateTicketTypes(request.ticketTypes());
    }

    private void validateLayout(List<String> rows, int perRow, long capacity) {
        if (rows.isEmpty() || new HashSet<>(rows).size() != rows.size()) {
            throw invalid("座位配置與場次容量不一致");
        }
        if (isInRange(perRow, 1, 99) || (long) rows.size() * perRow != capacity) {
            throw invalid("座位配置與場次容量不一致");
        }
    }

    private void validateConfiguration(SalesSettingsRequest request) {
        if (isInRange(request.maxTicketsPerMember(), 1, 10000)
                || isValidListSize(request.zones(), 150)
                || isValidListSize(request.ticketTypes(), 30)) {
            throw invalid("限購、分區或票種設定無效");
        }
    }

    private void validateZones(List<ZoneRequest> zones, List<String> rows) {
        int[] coverage = new int[rows.size()];
        Set<String> names = new HashSet<>();
        for (ZoneRequest zone : zones) {
            validateZone(zone, names);
            int start = rows.indexOf(zone.rowStart()), end = rows.indexOf(zone.rowEnd());
            if (start < 0 || end < start) throw invalid("分區排數範圍無效");
            for (int i = start; i <= end; i++) coverage[i]++;
        }
        if (Arrays.stream(coverage).anyMatch(count -> count != 1)) throw invalid("分區不得重疊，且必須涵蓋所有座位");
    }

    private void validateZone(ZoneRequest zone, Set<String> names) {
        if (zone == null) {
            throw invalid("分區名稱、顏色或價格無效");
        }
        if (isUniqueName(zone.name(), names)
                || !isValidColor(zone.color())
                || isValidDecimal(zone.price(), BigDecimal.ZERO, new BigDecimal("9999999999.99"))) {
            throw invalid("分區名稱、顏色或價格無效");
        }
    }

    private void validateTicketTypes(List<TicketTypeRequest> ticketTypes) {
        Set<String> names = new HashSet<>();
        for (TicketTypeRequest type : ticketTypes) {
            if (type == null) {
                throw invalid("票種名稱、價格比例或資格說明無效");
            }
            if (isUniqueName(type.name(), names)
                    || isValidDecimal(type.pricePercent(), new BigDecimal("0.01"), new BigDecimal("100"))
                    || (type.eligibility() != null && type.eligibility().length() > 200)) {
                throw invalid("票種名稱、價格比例或資格說明無效");
            }
        }
    }

    private boolean isInRange(Integer value, int min, int max) {
        return value == null || value < min || value > max;
    }

    private boolean isValidListSize(List<?> values, int max) {
        return values == null || values.isEmpty() || values.size() > max;
    }

    private boolean isUniqueName(String name, Set<String> names) {
        return isInvalidName(name) || !names.add(name.trim());
    }

    private boolean isValidColor(String color) {
        return color != null && color.matches("#[0-9a-fA-F]{6}");
    }

    private boolean isValidDecimal(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value == null || value.compareTo(min) < 0
                || value.compareTo(max) > 0 || value.scale() > 2;
    }

    private Map<String, Object> requireSession(String id) {
        Map<String, Object> session = salesSettingsMapper.session(id);
        if (session == null) {
            throw new BookingException("SESSION_NOT_FOUND", "找不到場次", HttpStatus.NOT_FOUND);
        }
        return session;
    }

    private List<String> rows(Map<String, Object> layout) {
        return layout.get("seat_rows") == null
                ? List.of()
                : Arrays.stream(
                        layout.get("seat_rows").toString().split(",")
                )
                .map(String::trim)
                .filter(row -> !row.isEmpty()).toList();
    }

    private List<String> seats(List<String> rows, int perRow) {
        List<String> seats = new ArrayList<>();
        for (String row : rows) {
            for (int i = 1; i <= perRow; i++) {
                seats.add("%s-%02d".formatted(row, i));
            }
        }
        return seats;
    }

    private int integer(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private boolean isLocked(String sessionId, Map<String, Object> session) {
        return integer(session.get("reserved")) > 0
                || integer(session.get("sold")) > 0
                || salesSettingsMapper.occupied(sessionId) > 0;
    }

    private boolean isInvalidName(String name) {
        return name == null || name.isBlank() || name.length() > 40;
    }

    private BookingException invalid(String message) {
        return new BookingException("INVALID_SALES_SETTINGS", message, HttpStatus.BAD_REQUEST);
    }

}
