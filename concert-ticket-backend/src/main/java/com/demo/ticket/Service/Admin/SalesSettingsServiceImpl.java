// 閱讀導引：管理場次分區、票種與限購設定；訂票時依資料庫設定計價，並檢查會員可購張數。
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

    /**
     * 注入售票設定查詢與場次鎖定介面，讓配置變更和訂票遵守相同鎖定流程。
     */
    public SalesSettingsServiceImpl(SalesSettingsMapper salesSettingsMapper, BookingCoreMapper bookingCoreMapper) {
        this.salesSettingsMapper = salesSettingsMapper;
        this.bookingCoreMapper = bookingCoreMapper;
    }

    /**
     * 在 REPEATABLE_READ 唯讀交易中讀取設定，讓場次、分區與票種查詢維持同一資料快照；不附會員額度。
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public Map<String, Object> readAdmin(String sessionId) {
        return readSettings(sessionId, null);
    }

    /**
     * 使用一致的讀取快照回傳票價及座位配置，另計入會員已付款與尚未逾期的待付款票數，計算剩餘額度。
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Override
    @PreAuthorize("hasAuthority('USER_ITEM_IMPLEMENT')")
    public Map<String, Object> readBooking(String sessionId, String email) {
        return readSettings(sessionId, email);
    }

    /**
     * 合併場次、分區、票種與會員數量，組裝售票設定；未配置時使用活動預設票價。
     */
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

    /**
     * 先鎖場次並核對草稿版本，再拒絕已有保留、售出或占用紀錄的場次。
     * 座位排號與每排座位數沿用既有配置；在同一交易內重建分區、票種及座位分區關聯，版本加一。
     * @param sessionId 要修改的場次
     * @param request 草稿版本、會員限購張數、完整分區及票種清單
     * @return 儲存後的完整售票設定
     * @throws BookingException 場次不存在、版本已更新、設定已鎖定或配置不合法
     */
    @Transactional
    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public Map<String, Object> save(String sessionId, SalesSettingsRequest request) {
        bookingCoreMapper.lockSession(sessionId);
        Map<String, Object> session = requireSession(sessionId);
        Map<String, Object> saved = salesSettingsMapper.settings(sessionId);
        long version = saved == null ? 0 : ((Number) saved.get("version")).longValue();
        // 版本號避免舊草稿覆蓋其他管理員剛儲存的設定。
        if (request.version() != version)
            throw new BookingException("SETTINGS_VERSION_CONFLICT", "設定已由其他管理員更新，請重新載入", HttpStatus.CONFLICT);
        // 已有保留或售出座位時凍結設定，維持既有訂單的座位及票價語意。
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

    /**
     * 未配置分區時使用活動預設價；已配置時確認座位與票種屬於場次，並檢查會員限購額度。
     * 票價以分區原價乘票種百分比計算，分區及票種名稱一起回傳，供訂單保存下單時資訊。
     * 呼叫端必須在交易內先取得場次鎖，避免限購檢查與建立訂單之間有其他訂票插入。
     */
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

    /**
     * 以分區原價乘上票種百分比，使用 HALF_UP 四捨五入到小數點後兩位。
     */
    private BigDecimal calculatePrice(BigDecimal price, BigDecimal percent) {
        // 使用十進位金額計算分區價格 × 票種百分比，四捨五入到小數點後兩位。
        return price.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * 依序驗證座位配置、限購、分區與票種；不合法時拋出設定驗證例外。
     */
    private void validate(SalesSettingsRequest request, List<String> rows, int perRow, long capacity) {
        validateLayout(rows, perRow, capacity);
        validateConfiguration(request);
        validateZones(request.zones(), rows);
        validateTicketTypes(request.ticketTypes());
    }

    /**
     * 確認排號唯一、每排座位數合法，且座位總數與場次容量一致。
     */
    private void validateLayout(List<String> rows, int perRow, long capacity) {
        if (rows.isEmpty() || new HashSet<>(rows).size() != rows.size()) {
            throw invalid("座位配置與場次容量不一致");
        }
        if (isInRange(perRow, 1, 99) || (long) rows.size() * perRow != capacity) {
            throw invalid("座位配置與場次容量不一致");
        }
    }

    /**
     * 確認限購張數、分區數與票種數皆在允許範圍內。
     */
    private void validateConfiguration(SalesSettingsRequest request) {
        if (isInRange(request.maxTicketsPerMember(), 1, 10000)
                || isValidListSize(request.zones(), 150)
                || isValidListSize(request.ticketTypes(), 30)) {
            throw invalid("限購、分區或票種設定無效");
        }
    }

    /**
     * 檢查分區資料及排數涵蓋範圍，要求每排恰好屬於一個分區。
     */
    private void validateZones(List<ZoneRequest> zones, List<String> rows) {
        // 每排必須恰好屬於一個分區；零次表示漏排，多次表示重疊。
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

    /**
     * 檢查單一分區的名稱、顏色與價格，並記錄名稱以偵測重複。
     */
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

    /**
     * 檢查票種名稱、價格比例及資格說明，拒絕不合法或重複設定。
     */
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

    /**
     * 檢查整數是否缺值或超出範圍；true 表示不合法，false 表示通過。
     */
    private boolean isInRange(Integer value, int min, int max) {
        return value == null || value < min || value > max;
    }

    /**
     * 檢查清單是否缺值、空白或超過上限；true 表示不合法。
     */
    private boolean isValidListSize(List<?> values, int max) {
        return values == null || values.isEmpty() || values.size() > max;
    }

    /**
     * 檢查名稱是否不合法或已重複；true 表示不合法，合法的新名稱會加入 names。
     */
    private boolean isUniqueName(String name, Set<String> names) {
        return isInvalidName(name) || !names.add(name.trim());
    }

    /**
     * 檢查顏色是否為六位十六進位色碼，true 表示合法。
     */
    private boolean isValidColor(String color) {
        return color != null && color.matches("#[0-9a-fA-F]{6}");
    }

    /**
     * 檢查數值是否缺值、超出範圍或超過兩位小數；true 表示不合法。
     */
    private boolean isValidDecimal(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value == null || value.compareTo(min) < 0
                || value.compareTo(max) > 0 || value.scale() > 2;
    }

    /**
     * 取得指定場次資料；不存在時拋出 HTTP 404 訂票例外。
     */
    private Map<String, Object> requireSession(String id) {
        Map<String, Object> session = salesSettingsMapper.session(id);
        if (session == null) {
            throw new BookingException("SESSION_NOT_FOUND", "找不到場次", HttpStatus.NOT_FOUND);
        }
        return session;
    }

    /**
     * 拆解逗號分隔的排號設定，移除空白後回傳有效排號清單。
     */
    private List<String> rows(Map<String, Object> layout) {
        return layout.get("seat_rows") == null
                ? List.of()
                : Arrays.stream(
                        layout.get("seat_rows").toString().split(",")
                )
                .map(String::trim)
                .filter(row -> !row.isEmpty()).toList();
    }

    /**
     * 依排號及每排座位數產生座位編號，例如 A-01。
     */
    private List<String> seats(List<String> rows, int perRow) {
        List<String> seats = new ArrayList<>();
        for (String row : rows) {
            for (int i = 1; i <= perRow; i++) {
                seats.add("%s-%02d".formatted(row, i));
            }
        }
        return seats;
    }

    /**
     * 將資料庫數值轉成整數；缺值時回傳 0。
     */
    private int integer(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    /**
     * 判斷場次是否已有保留或售出座位，true 表示不能再修改售票配置。
     */
    private boolean isLocked(String sessionId, Map<String, Object> session) {
        return integer(session.get("reserved")) > 0
                || integer(session.get("sold")) > 0
                || salesSettingsMapper.occupied(sessionId) > 0;
    }

    /**
     * 判斷名稱是否缺值、空白或超過 40 字；true 表示不合法。
     */
    private boolean isInvalidName(String name) {
        return name == null || name.isBlank() || name.length() > 40;
    }

    /**
     * 建立售票設定驗證例外，供呼叫端回傳 HTTP 400。
     */
    private BookingException invalid(String message) {
        return new BookingException("INVALID_SALES_SETTINGS", message, HttpStatus.BAD_REQUEST);
    }

}
