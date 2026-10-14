// 閱讀導引：活動與場次管理的服務契約；定義呼叫端可使用的操作，實際流程由對應實作類別負責。
package com.demo.ticket.Service.Admin;

import com.demo.ticket.Dto.Admin.AdminCreateSessionRequest;
import com.demo.ticket.Dto.Admin.AdminDeleteActivityRequest;
import com.demo.ticket.Dto.Admin.AdminSaveActivityRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface AdminService {

    /**
     * 查詢活動清單，回傳活動資訊供頁面呈現。
     */
    List<Map<String, Object>> selectAllActivities();

    /**
     * 查詢管理頁使用的場次清單及售票資訊。
     */
    List<Map<String, Object>> selectAllSessions();

    /**
     * 查詢管理頁使用的訂單清單。
     */
    List<Map<String, Object>> selectAllticket();

    /**
     * 接收活動資料與可省略的 JPEG 圖片，交由服務在交易內新增或更新；未上傳圖片時保留既有圖片。
     */
    Map<String, Object> saveActivity(@Valid AdminSaveActivityRequest request, MultipartFile image);

    /**
     * 依活動編號委派刪除；不存在回傳 404，已有場次回傳 409，成功時回傳更新後的活動清單。
     */
    ResponseEntity<?> deleteActivity(@Valid AdminDeleteActivityRequest request);

    /**
     * 新增或更新場次；空 id 表示新增，指定 id 表示編輯，回應提供完整場次清單。
     */
    ResponseEntity<?> createSession(@Valid AdminCreateSessionRequest request);
}
