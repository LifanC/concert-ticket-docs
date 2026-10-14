// 閱讀導引：訂票的服務契約；定義呼叫端可使用的操作，實際流程由對應實作類別負責。
package com.demo.ticket.Service.Booking;

import com.demo.ticket.Dto.Booking.*;
import com.demo.ticket.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface BookingService {

    /**
     * 依活動編號取得演出日期選單資料，供訂票第一步使用。
     */
    List<Map<String, Object>> selectOnlyActivities(BookingSelectOnlyActivitiesRequest request, LoginUser user);

    /**
     * 依活動與日期查詢可選場次。
     */
    List<Map<String, Object>> selectOnlySession(BookingSelectOnlySessionRequest request, LoginUser user);

    /**
     * 查詢目前會員的票券；會員身分來自登入資訊。
     */
    List<Map<String, Object>> selectOnlyTicket(LoginUser user);

    /**
     * 依活動編號查詢活動票價。
     */
    Map<String, Object> selectOnlyActivitiesPrice(BookingSelectOnlyActivitiesPriceRequest request, LoginUser user);

    /**
     * 以登入會員及 Idempotency-Key 提交一張座位的訂位；服務層計價並保留座位，相同識別碼與內容重試回傳原結果。
     */
    ResponseEntity<?> saveTicket(@Valid BookingSaveTicketRequest request, LoginUser user, String idempotencyKey);

    /**
     * 取消會員的待付款訂單；成功提交後撤銷逾期排程並通知會員。
     */
    ResponseEntity<?> cancelOrder(@Valid BookingCanceTicketRequest request, LoginUser user);

    /**
     * 依場次、活動、演出日期時間及待付款狀態，取得付款視窗所需的開賣資訊。
     */
    Map<String, Object> sessionSalesDate(@Valid BookingSessionSalesDateRequest request, LoginUser user);

    /**
     * 將會員訂單轉為已付款；這裡更新訂單狀態，未串接外部金流扣款。
     */
    ResponseEntity<?> dopayprice(@Valid BookingDopaypriceRequest request, LoginUser user);

    /**
     * 依活動佈局取得每個座位的編號、排號與座位數，供前端座位圖使用。
     */
    List<Map<String, Object>> selectOnlySeats(@Valid BookingSelectOnlySeatsRequest request, LoginUser user);

    /**
     * 查詢指定場次不可選的座位，供前端更新座位圖。
     */
    List<String> selectOnlyUnavailableSeats(@Valid BookingSelectOnlyUnavailableSeatsRequest request, LoginUser user);

}
