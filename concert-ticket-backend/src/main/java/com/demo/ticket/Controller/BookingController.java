// 閱讀導引：訂票的 HTTP 入口；接收與驗證請求後委派服務層執行，並組裝回應。
package com.demo.ticket.Controller;

import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.Service.Admin.SalesSettingsService;
import com.demo.ticket.Service.Booking.BookingService;
import com.demo.ticket.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final SalesSettingsService salesSettingsService;

    /**
     * 注入訂票與售票設定服務，分別處理下單操作及場次票價／限購查詢。
     */
    public BookingController(
            BookingService bookingService,
            SalesSettingsService salesSettingsService
    ){
        this.bookingService = bookingService;
        this.salesSettingsService = salesSettingsService;
    }

    /**
     * 依活動編號取得演出日期選單資料，供訂票第一步使用。
     */
    @Operation(summary = "1.活動資料", description = "活動資料")
    @GetMapping("/selectOnlyActivities")
    public List<Map<String, Object>> selectOnlyActivities(
            @ModelAttribute
            @Valid
            BookingSelectOnlyActivitiesRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.selectOnlyActivities(request, user);
    }

    /**
     * 依活動與日期查詢可選場次。
     */
    @Operation(summary = "1.場次資料", description = "場次資料")
    @GetMapping("/selectOnlySession")
    public List<Map<String, Object>> selectOnlySession(
            @ModelAttribute
            @Valid
            BookingSelectOnlySessionRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.selectOnlySession(request, user);
    }

    /**
     * 查詢目前會員的票券；會員身分來自登入資訊。
     */
    @Operation(summary = "1.訂單資料", description = "訂單資料")
    @GetMapping("/selectOnlyTicket")
    public List<Map<String, Object>> selectOnlyTicket(
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.selectOnlyTicket(user);
    }

    /**
     * 依活動編號查詢活動票價。
     */
    @Operation(summary = "2.場次金額", description = "場次金額")
    @GetMapping("/selectOnlyActivitiesPrice")
    public Map<String, Object> selectOnlyActivitiesPrice(
            @ModelAttribute
            @Valid
            BookingSelectOnlyActivitiesPriceRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.selectOnlyActivitiesPrice(request, user);
    }

    /**
     * 以登入會員及 Idempotency-Key 提交一張座位的訂位；服務層計價並保留座位，相同識別碼與內容重試回傳原結果。
     */
    @Operation(summary = "3.新增訂單", description = "新增訂單")
    @PostMapping("/saveTicket")
    public ResponseEntity<?> saveTicket(
            @Valid
            @RequestBody
            BookingSaveTicketRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.saveTicket(request, user, idempotencyKey);
    }

    /**
     * 取消會員的待付款訂單；成功提交後撤銷逾期排程並通知會員。
     */
    @Operation(summary = "4.取消訂單", description = "取消訂單")
    @PutMapping("/cancelOrder")
    public ResponseEntity<?> cancelOrder(
            @Valid
            @RequestBody
            BookingCanceTicketRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.cancelOrder(request, user);
    }

    /**
     * 依場次、活動、演出日期時間及待付款狀態，取得付款視窗所需的開賣資訊。
     */
    @Operation(summary = "5.售賣日期", description = "售賣日期")
    @PostMapping("/sessionSalesDate")
    public Map<String, Object> sessionSalesDate(
            @Valid
            @RequestBody
            BookingSessionSalesDateRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.sessionSalesDate(request, user);
    }

    /**
     * 將會員訂單轉為已付款；這裡更新訂單狀態，未串接外部金流扣款。
     */
    @Operation(summary = "6.付款", description = "付款")
    @PutMapping("/dopayprice")
    public ResponseEntity<?> dopayprice(
            @Valid
            @RequestBody
            BookingDopaypriceRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.dopayprice(request, user);
    }

    /**
     * 依活動佈局取得每個座位的編號、排號與座位數，供前端座位圖使用。
     */
    @Operation(summary = "7.座位資料", description = "座位資料")
    @GetMapping("/selectOnlySeats")
    public List<Map<String, Object>> selectOnlySeats(
            @ModelAttribute
            @Valid
            BookingSelectOnlySeatsRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.selectOnlySeats(request, user);
    }

    /**
     * 查詢指定場次不可選的座位，供前端更新座位圖。
     */
    @Operation(summary = "8.指定場次資料", description = "指定場次資料")
    @GetMapping("/selectOnlyUnavailableSeats")
    public List<String> selectOnlyUnavailableSeats(
            @ModelAttribute
            @Valid
            BookingSelectOnlyUnavailableSeatsRequest request,
            @AuthenticationPrincipal LoginUser user) {
        return bookingService.selectOnlyUnavailableSeats(request, user);
    }

    /**
     * 回傳訂票頁使用的售票設定，包含指定會員的已購數及剩餘額度。
     */
    @Operation(summary = "9.查詢訂票銷售設定", description = "會員查詢票價與剩餘可購張數")
    @GetMapping("/sessions/{sessionId}/sales-settings")
    public Map<String, Object> readBooking(
            @PathVariable String sessionId,
            @AuthenticationPrincipal LoginUser user) {
        return salesSettingsService.readBooking(sessionId, user.email());
    }

}
