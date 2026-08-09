package com.demo.ticket.Controller;

import com.demo.ticket.Dto.Booking.BookingCanceTicketRequest;
import com.demo.ticket.Dto.Booking.BookingDopaypriceRequest;
import com.demo.ticket.Dto.Booking.BookingSaveTicketRequest;
import com.demo.ticket.Dto.Booking.BookingSessionSalesDateRequest;
import com.demo.ticket.Service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Booking API", description = "訂票功能")
@RestController
@RequestMapping("/v1/booking")
@Validated
public class BookingController {

    private final BookingService bookingService;

    public BookingController(
            BookingService bookingService
    ){
        this.bookingService = bookingService;
    }

    @Operation(summary = "1.活動資料", description = "活動資料")
    @GetMapping("/selectOnlyActivities")
    public List<Map<String, Object>> selectOnlyActivities(
            @RequestParam String activity_name) {
        return bookingService.selectOnlyActivities(activity_name);
    }

    @Operation(summary = "1.場次資料", description = "場次資料")
    @GetMapping("/selectOnlySession")
    public List<Map<String, Object>> selectOnlySession(
            @RequestParam String date) {
        return bookingService.selectOnlySession(date);
    }

    @Operation(summary = "1.訂單資料", description = "訂單資料")
    @GetMapping("/selectOnlyTicket")
    public List<Map<String, Object>> selectOnlyTicket(
            @RequestParam String email) {
        return bookingService.selectOnlyTicket(email);
    }

    @Operation(summary = "2.場次金額", description = "場次金額")
    @GetMapping("/selectOnlyActivitiesPrice")
    public Map<String, Object> selectOnlyActivitiesPrice(
            @RequestParam String activity_id) {
        return bookingService.selectOnlyActivitiesPrice(activity_id);
    }

    @Operation(summary = "3.新增訂單", description = "新增訂單")
    @PostMapping("/saveTicket")
    public ResponseEntity<?> saveTicket(
            @Valid
            @RequestBody
            BookingSaveTicketRequest request,
            @Schema(description = "token", example = "token_abc123", requiredMode = Schema.RequiredMode.REQUIRED)
            @RequestHeader("Authorization") String authHeader) {
        request.setAuthHeader(authHeader);
        return bookingService.saveTicket(request);
    }

    @Operation(summary = "4.取消訂單", description = "取消訂單")
    @PutMapping("/cancelOrder")
    public ResponseEntity<?> cancelOrder(
            @Valid
            @RequestBody
            BookingCanceTicketRequest request,
            @Schema(description = "token", example = "token_abc123", requiredMode = Schema.RequiredMode.REQUIRED)
            @RequestHeader("Authorization") String authHeader) {
        request.setAuthHeader(authHeader);
        return bookingService.cancelOrder(request);
    }

    @Operation(summary = "5.售賣日期", description = "售賣日期")
    @PostMapping("/sessionSalesDate")
    public Map<String, Object> sessionSalesDate(
            @Valid
            @RequestBody
            BookingSessionSalesDateRequest request,
            @Schema(description = "token", example = "token_abc123", requiredMode = Schema.RequiredMode.REQUIRED)
            @RequestHeader("Authorization") String authHeader) {
        request.setAuthHeader(authHeader);
        return bookingService.sessionSalesDate(request);
    }

    @Operation(summary = "6.付款", description = "付款")
    @PutMapping("/dopayprice")
    public ResponseEntity<?> dopayprice(
            @Valid
            @RequestBody
            BookingDopaypriceRequest request,
            @Schema(description = "token", example = "token_abc123", requiredMode = Schema.RequiredMode.REQUIRED)
            @RequestHeader("Authorization") String authHeader) {
        request.setAuthHeader(authHeader);
        return bookingService.dopayprice(request);
    }

}
